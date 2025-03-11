package CloudSimTestRR;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

public class RoundRobinSimulation {
    private static DatacenterBroker brokerEven;
    private static DatacenterBroker brokerUneven;
    private SimulationConfig config;
    private Datacenter datacenter;

    // Track submission times for cloudlets
    private Map<Integer, Double> cloudletSubmissionTimes = new HashMap<>();

    // Track VM lists for each broker
    private List<Vm> vmListEven;
    private List<Vm> vmListUneven;

    public RoundRobinSimulation(SimulationConfig config) {
        this.config = config;
    }

    public String runSimulation() {
        System.out.println("Starting Load Balancing Simulation with Round Robin...");
        try {
            CloudSim.init(1, Calendar.getInstance(), false);
            datacenter = createDatacenter("Datacenter_1");

            brokerEven = createBroker("Broker_Even");
            brokerUneven = createBroker("Broker_Uneven");

            vmListEven = createVms(brokerEven.getId());
            vmListUneven = createVms(brokerUneven.getId());
            brokerEven.submitGuestList(vmListEven);
            brokerUneven.submitGuestList(vmListUneven);

            List<Cloudlet> cloudletListEven = createCloudlets(brokerEven.getId(), "Even");
            List<Cloudlet> cloudletListUneven = createCloudlets(brokerUneven.getId(), "Uneven");
            submitCloudlets(brokerEven, cloudletListEven);
            submitCloudlets(brokerUneven, cloudletListUneven);

            // Round Robin Load Balancing
            System.out.println("\n--- Running Round Robin Load Balancing for Even Workload ---");
            bindCloudletsToVmsRoundRobin(brokerEven, cloudletListEven, vmListEven);

            System.out.println("\n--- Running Round Robin Load Balancing for Uneven Workload ---");
            bindCloudletsToVmsRoundRobin(brokerUneven, cloudletListUneven, vmListUneven);

            CloudSim.startSimulation();

            // Collect and print results separately
            printCloudletResults("Even Workload", brokerEven.getCloudletReceivedList());
            printCloudletResults("Uneven Workload", brokerUneven.getCloudletReceivedList());

            CloudSim.stopSimulation();
            System.out.println("Simulation completed!");

            // Return results as JSON
            return getResultsAsJson();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Simulation terminated due to an error.");
            return "{\"error\": \"Simulation failed\"}";
        }
    }

    private Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        for (int i = 0; i < config.numHosts; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < config.numPesPerHost; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(config.peMips)));
            }
            hostList.add(new Host(i, new RamProvisionerSimple(config.ramPerHost), new BwProvisionerSimple(config.bwPerHost),
                    config.storagePerHost, peList, new VmSchedulerTimeShared(peList)));
        }

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList, 10.0, 3.0, 0.05, 0.001, 0.0);
        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static DatacenterBroker createBroker(String name) {
        try {
            return new DatacenterBroker(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Vm> createVms(int brokerId) {
        List<Vm> vmList = new ArrayList<>();
        for (int i = 0; i < config.numVms; i++) {
            vmList.add(new Vm(i, brokerId, config.vmMips, config.vmPes, config.vmRam, config.vmBw, config.vmSize, "Xen", new CloudletSchedulerTimeShared()));
        }
        return vmList;
    }

    private List<Cloudlet> createCloudlets(int brokerId, String workloadType) {
        List<Cloudlet> cloudletList = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < config.numCloudlets; i++) {
            long length = config.cloudletLength;
            if (workloadType.equals("Uneven")) {
                length = 20000 + rand.nextInt(80000);
            }
            Cloudlet cloudlet = new Cloudlet(i, length, config.cloudletPes, 300, 300,
                    new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }
        return cloudletList;
    }

    private void submitCloudlets(DatacenterBroker broker, List<Cloudlet> cloudletList) {
        double currentTime = CloudSim.clock();
        for (Cloudlet cloudlet : cloudletList) {
            cloudletSubmissionTimes.put(cloudlet.getCloudletId(), currentTime);
        }
        broker.submitCloudletList(cloudletList);
    }

    private static void bindCloudletsToVmsRoundRobin(DatacenterBroker broker, List<Cloudlet> cloudletList, List<Vm> vmList) {
        int vmIndex = 0;
        for (Cloudlet cloudlet : cloudletList) {
            Vm assignedVm = vmList.get(vmIndex % vmList.size());
            broker.bindCloudletToVm(cloudlet.getCloudletId(), assignedVm.getId());
            System.out.println("Cloudlet " + cloudlet.getCloudletId() + " assigned to VM " + assignedVm.getId());
            vmIndex++;
        }
    }

    private static void printCloudletResults(String method, List<Cloudlet> cloudletList) {
        System.out.println("\nResults for " + method + " Load Balancing");
        System.out.println("Cloudlet ID\tStatus\tVM ID\tTime\tStart Time\tFinish Time");
        for (Cloudlet cloudlet : cloudletList) {
            System.out.printf("%d\t\t%s\t%d\t%.2f\t%.2f\t\t%.2f\n",
                    cloudlet.getCloudletId(), cloudlet.getCloudletStatusString(), cloudlet.getGuestId(),
                    cloudlet.getActualCPUTime(), cloudlet.getExecStartTime(), cloudlet.getExecFinishTime());
        }
    }

    private Map<String, Object> calculateResponseTime(List<Cloudlet> cloudletList) {
        Map<String, Object> responseTimeMetrics = new HashMap<>();
        double totalResponseTime = 0;
        int totalCloudlets = cloudletList.size();

        for (Cloudlet cloudlet : cloudletList) {
            double submissionTime = cloudletSubmissionTimes.get(cloudlet.getCloudletId());
            double finishTime = cloudlet.getExecFinishTime();
            double responseTime = finishTime - submissionTime;
            totalResponseTime += responseTime;
        }

        responseTimeMetrics.put("averageResponseTime", totalResponseTime / totalCloudlets);
        return responseTimeMetrics;
    }

    private Map<String, Double> calculateHostResourceUtilization(List<Host> hosts) {
        Map<String, Double> utilization = new HashMap<>();
        double totalCpuUtilization = 0;
        double totalRamUtilization = 0;
        double totalBwUtilization = 0;
        int totalHosts = hosts.size();

        for (Host host : hosts) {
            // Calculate CPU utilization
            double hostCpuCapacity = host.getTotalMips();
            double hostCpuUsed = 0;

            // Sum up allocated MIPS for all VMs on this host
            for (GuestEntity vm : host.getGuestList()) {
                hostCpuUsed += host.getTotalAllocatedMipsForGuest(vm);
            }
            totalCpuUtilization += (hostCpuUsed / hostCpuCapacity) * 100;

            // Calculate RAM utilization
            double hostRamCapacity = host.getGuestRamProvisioner().getRam();
            double hostRamUsed = host.getGuestRamProvisioner().getUsedRam();
            totalRamUtilization += (hostRamUsed / hostRamCapacity) * 100;

            // Calculate bandwidth utilization
            double hostBwCapacity = host.getGuestBwProvisioner().getBw();
            double hostBwUsed = host.getGuestBwProvisioner().getUsedBw();
            totalBwUtilization += (hostBwUsed / hostBwCapacity) * 100;
        }

        // Calculate averages
        utilization.put("averageCpuUtilization", totalCpuUtilization / totalHosts);
        utilization.put("averageRamUtilization", totalRamUtilization / totalHosts);
        utilization.put("averageBwUtilization", totalBwUtilization / totalHosts);

        return utilization;
    }

    private Map<String, Double> calculateVmResourceUtilization(List<Vm> vms) {
        Map<String, Double> utilization = new HashMap<>();
        double totalCpuUtilization = 0;
        double totalRamUtilization = 0;
        double totalBwUtilization = 0;
        int totalVms = vms.size();

        for (Vm vm : vms) {
            double vmCpuCapacity = vm.getMips() * vm.getNumberOfPes();
            double vmCpuUsed = vm.getTotalUtilizationOfCpu(CloudSim.clock());
            totalCpuUtilization += (vmCpuUsed / vmCpuCapacity) * 100;

            double vmRamCapacity = vm.getRam();
            double vmRamUsed = vm.getGuestRamProvisioner().getUsedRam();
            totalRamUtilization += (vmRamUsed / vmRamCapacity) * 100;

            double vmBwCapacity = vm.getBw();
            double vmBwUsed = vm.getGuestBwProvisioner().getUsedBw();
            totalBwUtilization += (vmBwUsed / vmBwCapacity) * 100;
        }

        utilization.put("averageCpuUtilization", totalCpuUtilization / totalVms);
        utilization.put("averageRamUtilization", totalRamUtilization / totalVms);
        utilization.put("averageBwUtilization", totalBwUtilization / totalVms);

        return utilization;
    }

    public String getResultsAsJson() {
        Map<String, Object> results = new HashMap<>();

        // Add cloudlet results
        results.put("evenResults", formatCloudletResults(brokerEven.getCloudletReceivedList()));
        results.put("unevenResults", formatCloudletResults(brokerUneven.getCloudletReceivedList()));

        // Add response time
        results.put("responseTimeEven", calculateResponseTime(brokerEven.getCloudletReceivedList()));
        results.put("responseTimeUneven", calculateResponseTime(brokerUneven.getCloudletReceivedList()));

        // Add resource utilization
        List<Host> hosts = datacenter.getHostList();
        results.put("hostResourceUtilization", calculateHostResourceUtilization(hosts));

        results.put("vmResourceUtilizationEven", calculateVmResourceUtilization(vmListEven));
        results.put("vmResourceUtilizationUneven", calculateVmResourceUtilization(vmListUneven));

        // Convert to JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(results);
    }

    private static List<Map<String, Object>> formatCloudletResults(List<Cloudlet> cloudletList) {
        List<Map<String, Object>> formattedResults = new ArrayList<>();
        for (Cloudlet cloudlet : cloudletList) {
            Map<String, Object> cloudletData = new HashMap<>();
            cloudletData.put("cloudletId", cloudlet.getCloudletId());
            cloudletData.put("status", cloudlet.getCloudletStatusString());
            cloudletData.put("vmId", cloudlet.getGuestId());
            cloudletData.put("cpuTime", cloudlet.getActualCPUTime());
            cloudletData.put("startTime", cloudlet.getExecStartTime());
            cloudletData.put("finishTime", cloudlet.getExecFinishTime());
            formattedResults.add(cloudletData);
        }
        return formattedResults;
    }
}