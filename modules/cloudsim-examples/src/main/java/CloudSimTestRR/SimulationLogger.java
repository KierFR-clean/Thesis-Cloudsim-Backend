package CloudSimTestRR;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SimulationLogger {
    private ByteArrayOutputStream logStream;
    private PrintStream originalOut;
    private List<Map<String, Object>> schedulingLog; // Store assignments, migrations, power-offs
    private Map<String, Object> simulationResults;   // Store final results (energy, utilization, etc.)
    private Map<String, Object> simulationConfig;    // Store simulation configuration

    public SimulationLogger() {
        this.schedulingLog = new ArrayList<>();
        this.simulationResults = new HashMap<>();
        this.simulationConfig = new HashMap<>();
    }

    public void startLogging() {
        logStream = new ByteArrayOutputStream();
        PrintStream logPrintStream = new PrintStream(logStream);
        originalOut = System.out;
        System.setOut(logPrintStream);
    }

    public void stopLogging() {
        System.setOut(originalOut);
    }

    // Log the number of tasks (cloudlets) being scheduled
    public void logTaskCount(int taskCount, double currentTime) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("type", "taskCount");
        logEntry.put("taskCount", taskCount);
        logEntry.put("currentTime", currentTime);
        logEntry.put("description", String.format("Scheduling %d tasks at time %.2f seconds", taskCount, currentTime));
        schedulingLog.add(logEntry);
    }

    // Log cloudlet-to-VM assignment
    public void logAssignment(long cloudletId, long vmId, double submissionTime) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("type", "assignment");
        logEntry.put("cloudletId", cloudletId);
        logEntry.put("vmId", vmId);
        logEntry.put("submissionTime", submissionTime);
        logEntry.put("description", String.format("Cloudlet %d assigned to VM %d at submission time %.2f seconds",
                cloudletId, vmId, submissionTime));
        schedulingLog.add(logEntry);
    }

    // Log cloudlet migration between VMs
    public void logMigration(long cloudletId, long sourceVmId, long targetVmId, double migrationTime) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("type", "migration");
        logEntry.put("cloudletId", cloudletId);
        logEntry.put("sourceVmId", sourceVmId);
        logEntry.put("targetVmId", targetVmId);
        logEntry.put("migrationTime", migrationTime);
        logEntry.put("description", String.format("Cloudlet %d migrated from VM %d to VM %d at %.2f seconds",
                cloudletId, sourceVmId, targetVmId, migrationTime));
        schedulingLog.add(logEntry);
    }

    // Log host power-off
    public void logHostPowerOff(long hostId, double powerOffTime) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("type", "hostPowerOff");
        logEntry.put("hostId", hostId);
        logEntry.put("powerOffTime", powerOffTime);
        logEntry.put("description", String.format("Host %d powered off at %.2f seconds", hostId, powerOffTime));
        schedulingLog.add(logEntry);
    }

    // Log simulation configuration
    public void logSimulationConfig(SimulationConfig config) {
        simulationConfig.put("numHosts", config.numHosts);
        simulationConfig.put("numPesPerHost", config.numPesPerHost);
        simulationConfig.put("peMips", config.peMips);
        simulationConfig.put("ramPerHost", config.ramPerHost);
        simulationConfig.put("bwPerHost", config.bwPerHost);
        simulationConfig.put("storagePerHost", config.storagePerHost);
        simulationConfig.put("numVms", config.numVms);
        simulationConfig.put("vmPes", config.vmPes);
        simulationConfig.put("vmMips", config.vmMips);
        simulationConfig.put("vmRam", config.vmRam);
        simulationConfig.put("vmBw", config.vmBw);
        simulationConfig.put("vmSize", config.vmSize);
        simulationConfig.put("vmScheduler", config.vmScheduler);
        simulationConfig.put("numCloudlets", config.numCloudlets);
        simulationConfig.put("cloudletLength", config.cloudletLength);
        simulationConfig.put("cloudletPes", config.cloudletPes);
        simulationConfig.put("workloadType", config.workloadType);
        simulationConfig.put("optimizationAlgorithm", config.optimizationAlgorithm);
    }

    // Log final simulation results (energy consumption, utilization, power)
    public void logSimulationResults(double totalEnergy, List<Map<String, Object>> hostMetrics) {
        simulationResults.put("totalEnergyWh", totalEnergy);
        simulationResults.put("hosts", hostMetrics);
    }

    // Helper to create host metrics entry
    public Map<String, Object> createHostMetrics(long hostId, double utilization, double power, double energy) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("hostId", hostId);
        metrics.put("averageUtilization", utilization);
        metrics.put("powerW", power);
        metrics.put("energyWh", energy);
        return metrics;
    }

    // Return all logs as a JSON array
    public String getSchedulingLogAsJson() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeSpecialFloatingPointValues()
                .create();
        
        // Create a list to hold all log entries as an array
        List<Map<String, Object>> fullLog = new ArrayList<>();

        // Add configuration as a single entry
        Map<String, Object> configEntry = new HashMap<>();
        configEntry.put("type", "configuration");
        configEntry.put("data", simulationConfig);
        fullLog.add(configEntry);

        // Add all scheduling events
        fullLog.addAll(schedulingLog);

        // Add simulation results as a single entry
        Map<String, Object> resultsEntry = new HashMap<>();
        resultsEntry.put("type", "simulationResults");
        resultsEntry.put("data", simulationResults);
        fullLog.add(resultsEntry);

        return gson.toJson(fullLog);
    }

    public String getLogs() {
        return logStream.toString();
    }
}