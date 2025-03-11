package CloudSimTestRR;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.HostEntity;
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
    
    // Track VM per host
    private Map<Host, List<Vm>> vmsPerHostMap = new HashMap<>();

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

            // Calculate resource utilization using the locally stored map
            System.out.println("\nCalculating Host and VM Resource Utilization...");
            Map<String, Double> hostUtilization = calculateHostResourceUtilization();
            Map<String, Double> vmUtilizationEven = calculateVmResourceUtilization(vmListEven);
            Map<String, Double> vmUtilizationUneven = calculateVmResourceUtilization(vmListUneven);

            // Print the resource utilization results
            System.out.println("Host Resource Utilization: " + hostUtilization);
            System.out.println("Even VM Resource Utilization: " + vmUtilizationEven);
            System.out.println("Uneven VM Resource Utilization: " + vmUtilizationUneven);

            // Stop the simulation AFTER calculating utilization
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
            Host host = new Host(i, new RamProvisionerSimple(config.ramPerHost), new BwProvisionerSimple(config.bwPerHost),
                    config.storagePerHost, peList, new VmSchedulerTimeShared(peList));
            hostList.add(host);

            vmsPerHostMap.put(host, new ArrayList<>());
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
            Vm vm = new Vm(
                    i, brokerId, config.vmMips, config.vmPes, 
                    config.vmRam, config.vmBw, config.vmSize, "Xen", 
                    new CloudletSchedulerDynamicWorkload(config.vmMips, config.vmPes)
            );

            // Explicitly set RamProvisioner and BwProvisioner
            vm.setGuestRamProvisioner(new RamProvisionerSimple(config.vmRam));
            vm.setGuestBwProvisioner(new BwProvisionerSimple(config.vmBw));

            vmList.add(vm);

            // Assign the VM to a host (round-robin assignment)
            HostEntity host = datacenter.getHostList().get(i % config.numHosts);
            vmsPerHostMap.get(host).add(vm); // Track the VM in the map
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
                    new UtilizationModelStochastic(), new UtilizationModelStochastic(), new UtilizationModelStochastic());

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

    private Map<String, Double> calculateHostResourceUtilization() {
        Map<String, Double> utilization = new HashMap<>();
        double totalCpuUtilization = 0;
        double totalRamUtilization = 0;
        double totalBwUtilization = 0;
        int utilizedHosts = 0;

        for (Map.Entry<Host, List<Vm>> entry : vmsPerHostMap.entrySet()) {
            Host host = entry.getKey();
            List<Vm> vmsOnHost = entry.getValue();

            // Debugging: Print host ID and number of VMs
            System.out.println("Host ID: " + host.getId() + " | VMs: " + vmsOnHost.size());

            if (vmsOnHost.isEmpty()) {
                System.out.println("Host ID: " + host.getId() + " has no VMs. Skipping...");
                continue; // Skip hosts that have no active VMs
            }

            utilizedHosts++;

            // Calculate CPU utilization based on VMs
            double hostCpuCapacity = host.getTotalMips();
            double hostCpuUsed = 0;

            // Debugging: Print host CPU capacity
            System.out.println("Host ID: " + host.getId() + " | CPU Capacity: " + hostCpuCapacity);

            for (Vm vm : vmsOnHost) {
                // Sum up the CPU usage of all VMs on this host
                double vmCpuUsage = vm.getMips() * vm.getNumberOfPes();
                hostCpuUsed += vmCpuUsage;

                // Debugging: Print VM CPU usage
                System.out.println("VM ID: " + vm.getId() + " | CPU Usage: " + vmCpuUsage);
            }

            double cpuUtilizationPercentage = (hostCpuCapacity > 0) ? (hostCpuUsed / hostCpuCapacity) * 100 : 0;
            totalCpuUtilization += cpuUtilizationPercentage;

            // Debugging: Print host CPU utilization
            System.out.println("Host ID: " + host.getId() + " | CPU Utilization: " + cpuUtilizationPercentage + "%");

            // Calculate RAM utilization based on VMs
            double hostRamCapacity = host.getRam();
            double hostRamUsed = 0;

            // Debugging: Print host RAM capacity
            System.out.println("Host ID: " + host.getId() + " | RAM Capacity: " + hostRamCapacity);

            for (Vm vm : vmsOnHost) {
                // Sum up the RAM usage of all VMs on this host
                double vmRamUsage = vm.getRam();
                hostRamUsed += vmRamUsage;

                // Debugging: Print VM RAM usage
                System.out.println("VM ID: " + vm.getId() + " | RAM Usage: " + vmRamUsage);
            }

            double ramUtilizationPercentage = (hostRamCapacity > 0) ? (hostRamUsed / hostRamCapacity) * 100 : 0;
            totalRamUtilization += ramUtilizationPercentage;

            // Debugging: Print host RAM utilization
            System.out.println("Host ID: " + host.getId() + " | RAM Utilization: " + ramUtilizationPercentage + "%");

            // Calculate bandwidth utilization based on VMs
            double hostBwCapacity = host.getBw();
            double hostBwUsed = 0;

            // Debugging: Print host bandwidth capacity
            System.out.println("Host ID: " + host.getId() + " | BW Capacity: " + hostBwCapacity);

            for (Vm vm : vmsOnHost) {
                // Sum up the bandwidth usage of all VMs on this host
                double vmBwUsage = vm.getBw();
                hostBwUsed += vmBwUsage;

                // Debugging: Print VM bandwidth usage
                System.out.println("VM ID: " + vm.getId() + " | BW Usage: " + vmBwUsage);
            }

            double bwUtilizationPercentage = (hostBwCapacity > 0) ? (hostBwUsed / hostBwCapacity) * 100 : 0;
            totalBwUtilization += bwUtilizationPercentage;

            // Debugging: Print host bandwidth utilization
            System.out.println("Host ID: " + host.getId() + " | BW Utilization: " + bwUtilizationPercentage + "%");
        }

        // Calculate averages based on only utilized hosts
        double avgCpuUtilization = utilizedHosts > 0 ? totalCpuUtilization / utilizedHosts : 0.0;
        double avgRamUtilization = utilizedHosts > 0 ? totalRamUtilization / utilizedHosts : 0.0;
        double avgBwUtilization = utilizedHosts > 0 ? totalBwUtilization / utilizedHosts : 0.0;

        utilization.put("averageCpuUtilization", avgCpuUtilization);
        utilization.put("averageRamUtilization", avgRamUtilization);
        utilization.put("averageBwUtilization", avgBwUtilization);

        // Debugging: Print final utilization metrics
        System.out.println("Final Host Utilization Metrics:");
        System.out.println("Average CPU Utilization: " + avgCpuUtilization + "%");
        System.out.println("Average RAM Utilization: " + avgRamUtilization + "%");
        System.out.println("Average BW Utilization: " + avgBwUtilization + "%");

        return utilization;
    }


    private Map<String, Double> calculateVmResourceUtilization(List<Vm> vms) {
        Map<String, Double> utilization = new HashMap<>();
        double totalCpuUtilizationEven = 0;
        double totalRamUtilizationEven = 0;
        double totalBwUtilizationEven = 0;
        double totalCpuUtilizationUneven = 0;
        double totalRamUtilizationUneven = 0;
        double totalBwUtilizationUneven = 0;
        int totalVms = vms.size();

        // Get the current simulation time
        double currentTime = CloudSim.clock(); 

        for (Vm vm : vms) {
            double vmCpuCapacity = vm.getMips() * vm.getNumberOfPes();
            double vmCpuUsedEven = 0;
            double vmCpuUsedUneven = 0;
           

            // Sum up the CPU utilization of all cloudlets assigned to this VM for even broker
            for (Cloudlet cloudlet : brokerEven.getCloudletReceivedList()) {
                if (cloudlet.getGuestId() == vm.getId()) {
                    vmCpuUsedEven += cloudlet.getActualCPUTime();  // Approximate CPU usage
                }
            }

            // Sum up the CPU utilization of all cloudlets assigned to this VM for uneven broker
            for (Cloudlet cloudlet : brokerUneven.getCloudletReceivedList()) {
                if (cloudlet.getGuestId() == vm.getId()) {
                    vmCpuUsedUneven += cloudlet.getActualCPUTime();
                }
            }

            double cpuUtilizationPercentageEven = (vmCpuCapacity > 0) ? (vmCpuUsedEven / vmCpuCapacity) * 100 : 0;
            double cpuUtilizationPercentageUneven = (vmCpuCapacity > 0) ? (vmCpuUsedUneven / vmCpuCapacity) * 100 : 0;
            totalCpuUtilizationEven += cpuUtilizationPercentageEven;
            totalCpuUtilizationUneven += cpuUtilizationPercentageUneven;

            // RAM Utilization
            double vmRamCapacity = vm.getRam();
            double vmRamUsedEven = 0;
            double vmRamUsedUneven = 0;

            // Calculate RAM used by cloudlets from even broker
            for (Cloudlet cloudlet : brokerEven.getCloudletReceivedList()) {
                if (cloudlet.getGuestId() == vm.getId()) {
                    vmRamUsedEven += cloudlet.getUtilizationOfRam(currentTime);  // Pass current simulation time
                }
            }

            // Calculate RAM used by cloudlets from uneven broker
            for (Cloudlet cloudlet : brokerUneven.getCloudletReceivedList()) {
                if (cloudlet.getGuestId() == vm.getId()) {
                    vmRamUsedUneven += cloudlet.getUtilizationOfRam(currentTime);  // Pass current simulation time
                }
            }

            double ramUtilizationPercentageEven = (vmRamCapacity > 0) ? (vmRamUsedEven / vmRamCapacity) * 100 : 0;
            double ramUtilizationPercentageUneven = (vmRamCapacity > 0) ? (vmRamUsedUneven / vmRamCapacity) * 100 : 0;
            totalRamUtilizationEven += ramUtilizationPercentageEven;
            totalRamUtilizationUneven += ramUtilizationPercentageUneven;

            // Bandwidth Utilization
            double vmBwCapacity = vm.getBw();
            double vmBwUsedEven = 0;
            double vmBwUsedUneven = 0;

            // Calculate bandwidth used by cloudlets from even broker
            for (Cloudlet cloudlet : brokerEven.getCloudletReceivedList()) {
                if (cloudlet.getGuestId() == vm.getId()) {
                    vmBwUsedEven += cloudlet.getUtilizationOfBw(currentTime);  // Pass current simulation time
                }
            }

            // Calculate bandwidth used by cloudlets from uneven broker
            for (Cloudlet cloudlet : brokerUneven.getCloudletReceivedList()) {
                if (cloudlet.getGuestId() == vm.getId()) {
                    vmBwUsedUneven += cloudlet.getUtilizationOfBw(currentTime);  // Pass current simulation time
                }
            }

            double bwUtilizationPercentageEven = (vmBwCapacity > 0) ? (vmBwUsedEven / vmBwCapacity) * 100 : 0;
            double bwUtilizationPercentageUneven = (vmBwCapacity > 0) ? (vmBwUsedUneven / vmBwCapacity) * 100 : 0;
            totalBwUtilizationEven += bwUtilizationPercentageEven;
            totalBwUtilizationUneven += bwUtilizationPercentageUneven;
        }

        // Compute average utilization for even broker
        double avgCpuUtilizationEven = totalVms > 0 ? totalCpuUtilizationEven / totalVms : 0.0;
        double avgRamUtilizationEven = totalVms > 0 ? totalRamUtilizationEven / totalVms : 0.0;
        double avgBwUtilizationEven = totalVms > 0 ? totalBwUtilizationEven / totalVms : 0.0;

        // Compute average utilization for uneven broker
        double avgCpuUtilizationUneven = totalVms > 0 ? totalCpuUtilizationUneven / totalVms : 0.0;
        double avgRamUtilizationUneven = totalVms > 0 ? totalRamUtilizationUneven / totalVms : 0.0;
        double avgBwUtilizationUneven = totalVms > 0 ? totalBwUtilizationUneven / totalVms : 0.0;

        // Store results in the map
        utilization.put("averageCpuUtilizationEven", avgCpuUtilizationEven);
        utilization.put("averageRamUtilizationEven", avgRamUtilizationEven);
        utilization.put("averageBwUtilizationEven", avgBwUtilizationEven);
        utilization.put("averageCpuUtilizationUneven", avgCpuUtilizationUneven);
        utilization.put("averageRamUtilizationUneven", avgRamUtilizationUneven);
        utilization.put("averageBwUtilizationUneven", avgBwUtilizationUneven);

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
        results.put("hostResourceUtilization", calculateHostResourceUtilization());

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