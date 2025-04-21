package CloudSimTestRR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

import com.google.gson.Gson;

public class ResultsFormatter {
    private static final double POWER_BUSY = 215.0; // Watts
    private static final double POWER_IDLE = 162.0; // Watts
    private SimulationConfig config;
    private SimulationLogger simulationLogger;
    private Map<Integer, Double> cloudletSubmissionTimes;
    private Map<Integer, Double> cloudletCpuRequests;
    private Map<Integer, List<Double>> hostUtilizationHistory;
    private Map<Long, Integer> vmHostMap;
    private Map<Integer, Boolean> hostActive; // New field to track powered-off state
    private double finalSimulationTime;

    public ResultsFormatter(SimulationConfig config, SimulationLogger simulationLogger,
                           Map<Integer, Double> cloudletSubmissionTimes,
                           Map<Integer, Double> cloudletCpuRequests,
                           Map<Integer, List<Double>> hostUtilizationHistory,
                           Map<Long, Integer> vmHostMap,
                           Map<Integer, Boolean> hostActive, // Add to constructor
                           double finalSimulationTime) {
        this.config = config;
        this.simulationLogger = simulationLogger;
        this.cloudletSubmissionTimes = cloudletSubmissionTimes;
        this.cloudletCpuRequests = cloudletCpuRequests;
        this.hostUtilizationHistory = hostUtilizationHistory;
        this.vmHostMap = vmHostMap;
        this.hostActive = hostActive;
        this.finalSimulationTime = finalSimulationTime;
    }

    public List<Map<String, Object>> formatCloudletResults(List<Cloudlet> cloudlets) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Cloudlet cloudlet : cloudlets) {
            Map<String, Object> cloudletData = new HashMap<>();
            cloudletData.put("cloudletId", cloudlet.getCloudletId());
            cloudletData.put("vmId", cloudlet.getVmId());
            cloudletData.put("submissionTime", cloudletSubmissionTimes.get(cloudlet.getCloudletId()));
            cloudletData.put("startTime", cloudlet.getExecStartTime());
            cloudletData.put("finishTime", cloudlet.getFinishTime());
            cloudletData.put("responseTime", cloudlet.getFinishTime() - cloudletSubmissionTimes.get(cloudlet.getCloudletId()));
            cloudletData.put("status", cloudlet.getStatus().toString());
            results.add(cloudletData);
        }
        return results;
    }

    public List<Map<String, Object>> calculateVmUtilization(List<Vm> vmList, List<Cloudlet> cloudlets,
                                                           List<Host> hosts) {
        List<Map<String, Object>> vmUtilizationResults = new ArrayList<>();
        Map<Long, Double> vmCpuUtilization = new HashMap<>();
        Map<Long, Double> vmRamUtilization = new HashMap<>();
        Map<Long, Integer> vmCloudletCount = new HashMap<>();

        for (Vm vm : vmList) {
            Long vmId = (long) vm.getId();
            vmCpuUtilization.put(vmId, 0.0);
            vmRamUtilization.put(vmId, 0.0);
            vmCloudletCount.put(vmId, 0);
        }

        for (Host host : hosts) {
            hostUtilizationHistory.putIfAbsent(host.getId(), new ArrayList<>());
        }

        for (Cloudlet cloudlet : cloudlets) {
            Long vmId = (long) cloudlet.getVmId();
            double cpuRequest = cloudletCpuRequests.get(cloudlet.getCloudletId());
            double cpuUtil = cpuRequest * config.vmMips;
            double ramUtil = cloudlet.getUtilizationOfRam(cloudlet.getFinishTime()) * config.vmRam;
            vmCpuUtilization.put(vmId, vmCpuUtilization.get(vmId) + cpuUtil);
            vmRamUtilization.put(vmId, vmRamUtilization.get(vmId) + ramUtil);
            vmCloudletCount.put(vmId, vmCloudletCount.get(vmId) + 1);

            Integer hostId = vmHostMap.get(vmId);
            if (hostId != null) {
                double hostUtil = cpuUtil / (config.peMips * config.numPesPerHost);
                hostUtilizationHistory.get(hostId).add(hostUtil);
                System.out.println("Cloudlet " + cloudlet.getCloudletId() + " VM " + vmId + " Host " + hostId + " cpuUtil: " + cpuUtil + " hostUtil: " + hostUtil);
            } else {
                System.out.println("Cloudlet " + cloudlet.getCloudletId() + " VM " + vmId + " has no host mapping");
            }
        }

        for (Vm vm : vmList) {
            Long vmId = (long) vm.getId();
            int count = vmCloudletCount.get(vmId);
            double avgCpuUtil = count > 0 ? vmCpuUtilization.get(vmId) / count : 0;
            double avgRamUtil = count > 0 ? vmRamUtilization.get(vmId) / count : 0;

            Map<String, Object> vmResult = new HashMap<>();
            vmResult.put("vmId", vmId);
            vmResult.put("cpuUtilization", avgCpuUtil);
            vmResult.put("ramUtilization", avgRamUtil);
            vmResult.put("numAPECloudlets", count);
            vmUtilizationResults.add(vmResult);
        }

        return vmUtilizationResults;
    }

    public Map<String, Object> calculateEnergyConsumption(List<Host> hosts) {
        Map<String, Object> energyResults = new HashMap<>();
        double totalEnergy = 0.0;
        List<Map<String, Object>> hostEnergyList = new ArrayList<>();

        for (Host host : hosts) {
            int hostId = host.getId();
            List<Double> utilizations = hostUtilizationHistory.getOrDefault(hostId, new ArrayList<>());
            double averageUtilization = utilizations.isEmpty() ? 0.0 :
                    utilizations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            averageUtilization = Math.min(averageUtilization, 1.0);

            // Check if host is powered off; if so, power = 0 W
            double power = 0.0;
            boolean isActive = hostActive.getOrDefault(hostId, true); // Default to true if not specified
            if (isActive) {
                power = averageUtilization > 0 ?
                        (POWER_BUSY - POWER_IDLE) * averageUtilization + POWER_IDLE : POWER_IDLE;
            }

            double simulationTime = finalSimulationTime / 3600.0;
            System.out.println("Simulation time: " + simulationTime + " s");
            double energy = power * simulationTime;

            totalEnergy += energy;

            Map<String, Object> hostEnergy = new HashMap<>();
            hostEnergy.put("hostId", hostId);
            hostEnergy.put("averageUtilization", averageUtilization);
            hostEnergy.put("power", power);
            hostEnergy.put("energy", energy);
            hostEnergyList.add(hostEnergy);
            System.out.println("Host " + hostId + " util: " + averageUtilization + " power: " + power + " energy: " + energy);
        }

        energyResults.put("totalEnergyWh", totalEnergy);
        energyResults.put("hosts", hostEnergyList);
        System.out.println("Total energy: " + totalEnergy + " Wh, simulation time: " + finalSimulationTime + " s");
        return energyResults;
    }

    public Map<String, Object> calculateSummary(List<Cloudlet> cloudlets) {
        Map<String, Object> summary = new HashMap<>();
        double totalResponseTime = 0;
        for (Cloudlet cloudlet : cloudlets) {
            double submissionTime = cloudletSubmissionTimes.get(cloudlet.getCloudletId());
            double finishTime = cloudlet.getFinishTime();
            double responseTime = Math.max(0.0, finishTime - submissionTime);
            totalResponseTime += responseTime;
        }
        summary.put("totalCloudlets", cloudlets.size());
        summary.put("finishedCloudlets", cloudlets.size());
        summary.put("averageResponseTime", cloudlets.isEmpty() ? 0 : totalResponseTime / cloudlets.size());
        return summary;
    }

    public List<Map<String, Object>> parseSchedulingLog() {
        Gson gson = new Gson();
        return gson.fromJson(simulationLogger.getSchedulingLogAsJson(), List.class);
    }
}