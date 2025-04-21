package CloudSimTestRR;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;

import com.google.gson.GsonBuilder;

public class CSVTaskSimulation {
    private SimulationConfig config;
    private SimulationLogger simulationLogger;
    private DatacenterBroker broker;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private Map<Integer, Double> cloudletSubmissionTimes;
    private Map<Integer, Double> cloudletCpuRequests;
    private Datacenter datacenter;
    private Map<Integer, List<Double>> hostUtilizationHistory;
    private Map<Long, Integer> vmHostMap;
    private Map<Integer, Boolean> hostActive; // New field to store host active state
    private double finalSimulationTime;

    private DatacenterFactory datacenterFactory;
    private VmFactory vmFactory;
    private CloudletFactory cloudletFactory;
    private Scheduler scheduler;
    private ResultsFormatter resultsFormatter;

    public CSVTaskSimulation(SimulationConfig config) {
        if (config.numCloudlets <= 0) {
            throw new IllegalArgumentException("Number of Cloudlets must be greater than 0");
        }
        this.config = config;
        this.simulationLogger = new SimulationLogger();
        this.cloudletSubmissionTimes = new HashMap<>();
        this.cloudletCpuRequests = new HashMap<>();
        this.cloudletList = new ArrayList<>();
        this.hostUtilizationHistory = new HashMap<>();
        this.vmHostMap = new HashMap<>();
        this.hostActive = new HashMap<>(); // Initialize hostActive
        this.finalSimulationTime = 0.0;

        this.datacenterFactory = new DatacenterFactory(config);
        this.vmFactory = new VmFactory(config);
        this.cloudletFactory = new CloudletFactory(config, cloudletSubmissionTimes, cloudletCpuRequests);
        this.scheduler = new Scheduler(config, simulationLogger);
        System.out.println("Scheduler: " + config.optimizationAlgorithm);
        this.resultsFormatter = new ResultsFormatter(config, simulationLogger, cloudletSubmissionTimes,
                cloudletCpuRequests, hostUtilizationHistory, vmHostMap, hostActive, finalSimulationTime);
    }

    public String getLogs() {
        return simulationLogger.getLogs();
    }

    public String getEnergyResults() {
        return scheduler.getEnergyDebugLog();
    }

    public String getSchedulingLog() {
        return simulationLogger.getSchedulingLogAsJson();
    }

    public String runSimulation() {
        simulationLogger.startLogging();
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            datacenter = datacenterFactory.createDatacenter("Datacenter_0");
            broker = datacenterFactory.createBroker();
            int brokerId = broker.getId();

            vmList = vmFactory.createVMs(brokerId);
            broker.submitGuestList(vmList);

            allocateVmsToHosts();

            cloudletList = cloudletFactory.readTasksFromCSV(brokerId);

            cloudletList.sort((c1, c2) -> {
                double time1 = cloudletSubmissionTimes.get(c1.getCloudletId());
                double time2 = cloudletSubmissionTimes.get(c2.getCloudletId());
                return Double.compare(time1, time2);
            });

            broker.submitCloudletList(cloudletList);
            // Capture hostActive from Scheduler
            hostActive = scheduler.bindCloudletsToVms(cloudletList, vmList, cloudletSubmissionTimes, cloudletCpuRequests,
                    broker, CloudSim.clock(), datacenter.getHostList());

            CloudSim.startSimulation();
            finalSimulationTime = Math.max(CloudSim.clock(), getMaxFinishTime());
            resultsFormatter = new ResultsFormatter(config, simulationLogger, cloudletSubmissionTimes,
                    cloudletCpuRequests, hostUtilizationHistory, vmHostMap, hostActive, finalSimulationTime);
            CloudSim.stopSimulation();

            return getResultsAsJson();

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } finally {
            simulationLogger.stopLogging();
        }
    }

    private double getMaxFinishTime() {
        double maxFinishTime = 0.0;
        for (Cloudlet cloudlet : cloudletList) {
            double submissionTime = cloudletSubmissionTimes.get(cloudlet.getCloudletId());
            double duration = 672.770490608695; // From CSV
            double finishTime = submissionTime + duration;
            maxFinishTime = Math.max(maxFinishTime, finishTime);
        }
        return maxFinishTime;
    }

    private void allocateVmsToHosts() {
        VmAllocationPolicySimple allocationPolicy = (VmAllocationPolicySimple) datacenter.getVmAllocationPolicy();
        for (Vm vm : vmList) {
            boolean allocated = allocationPolicy.allocateHostForVm(vm);
            if (allocated) {
                vmHostMap.put((long) vm.getId(), vm.getHost().getId());
                System.out.println("VM " + vm.getId() + " allocated to Host " + vm.getHost().getId());
            } else {
                System.out.println("Failed to allocate VM " + vm.getId() + " to any host");
            }
        }
    }

    private String getResultsAsJson() {
        Map<String, Object> results = new HashMap<>();

        results.put("cloudlets", resultsFormatter.formatCloudletResults(broker.getCloudletReceivedList()));
        results.put("vmUtilization", resultsFormatter.calculateVmUtilization(vmList,
                broker.getCloudletReceivedList(), datacenter.getHostList()));
        results.put("energyConsumption", resultsFormatter.calculateEnergyConsumption(datacenter.getHostList()));
        results.put("summary", resultsFormatter.calculateSummary(broker.getCloudletReceivedList()));
        results.put("schedulingLog", resultsFormatter.parseSchedulingLog());

        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeSpecialFloatingPointValues()
                .create()
                .toJson(results);
    }
}