package CloudSimTestRR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

public class Scheduler {

    private static final double POWER_BUSY = 215.0; // Page 42: Power model
    private static final double POWER_IDLE = 162.0; // Page 42
    private static final double MIGRATION_ENERGY_COST = 1.0; // Page 46: Migration cost in Wh
    private static final double MIGRATION_DOWNTIME = 0.05; // Page 46: Migration downtime in seconds
    private SimulationLogger simulationLogger;
    private SimulationConfig config;
    private Random random;
    private StringBuilder energyDebugLog; // Collect debug messages

    public Scheduler(SimulationConfig config, SimulationLogger simulationLogger) {
        this.config = config;
        this.simulationLogger = simulationLogger;
        this.random = new Random();
        this.energyDebugLog = new StringBuilder();
        // Log the simulation configuration at initialization
        simulationLogger.logSimulationConfig(config);
    }

    // Method to retrieve the collected debug log
    public String getEnergyDebugLog() {
        return energyDebugLog.toString();
    }

    // Helper method to append debug messages to the log
    private void appendDebugLog(String message) {
        energyDebugLog.append(message).append("\n");
    }

    public Map<Integer, Boolean> bindCloudletsToVms(List<Cloudlet> cloudletList, List<Vm> vmList,
            Map<Integer, Double> cloudletSubmissionTimes, Map<Integer, Double> cloudletCpuRequests,
            DatacenterBroker broker, double currentTime, List<Host> hostList) {
        // Log the number of tasks for verification
        simulationLogger.logTaskCount(cloudletList.size(), currentTime);

        Map<Integer, Boolean> hostActive;
        if (config.optimizationAlgorithm.equals("EPSO")) {
            hostActive = bindCloudletsToVmsEPSO(cloudletList, vmList, cloudletSubmissionTimes, cloudletCpuRequests, broker, currentTime, hostList);
        } else {
            hostActive = bindCloudletsToVmsRoundRobin(cloudletList, vmList, cloudletSubmissionTimes, broker, currentTime, hostList);
        }

        // After scheduling, calculate and log final energy consumption
        logFinalResults(cloudletList, vmList, cloudletCpuRequests, hostList, hostActive);
        return hostActive;
    }

    public Map<Integer, Boolean> bindCloudletsToVmsRoundRobin(List<Cloudlet> cloudletList, List<Vm> vmList,
            Map<Integer, Double> cloudletSubmissionTimes, DatacenterBroker broker, double currentTime, List<Host> hostList) {
        Map<Integer, Boolean> hostActive = new HashMap<>();
        // For RR, all hosts with assigned VMs are active (no power-off mechanism, Page 36)
        for (Host host : hostList) {
            hostActive.put(host.getId(), false); // Initialize as inactive
            appendDebugLog(" initialize  yehey: Host  = false");

        }

        int vmIndex = 0;
        for (Cloudlet cloudlet : cloudletList) {
            int vmId = vmList.get(vmIndex % vmList.size()).getId();
            broker.bindCloudletToVm(cloudlet.getCloudletId(), vmId);
            double submissionTime = cloudletSubmissionTimes.get(cloudlet.getCloudletId());
            cloudlet.setExecStartTime(submissionTime);
            simulationLogger.logAssignment(cloudlet.getCloudletId(), vmId, submissionTime);

            // Mark the host of the assigned VM as active
            Vm assignedVm = vmList.get(vmIndex % vmList.size());
            if (assignedVm.getHost() != null) {
                hostActive.put(assignedVm.getHost().getId(), true);
                appendDebugLog(" assigned  yehey: Host  = true");

            }
            vmIndex++;
        }
        return hostActive;
    }
    //still needs to be fixed since our epso needs tuning probably will be done in the 2 month dev 
    private Map<Integer, Boolean> bindCloudletsToVmsEPSO(List<Cloudlet> cloudletList, List<Vm> vmList,
            Map<Integer, Double> cloudletSubmissionTimes, Map<Integer, Double> cloudletCpuRequests,
            DatacenterBroker broker, double currentTime, List<Host> hostList) {
        // Page 45: EPSO optimizes task-to-VM mappings using PSO
        int numCloudlets = cloudletList.size();
        int numVms = vmList.size();
        int numParticles = 50; // Page 45: Number of particles (assumed value)
        int maxIterations = 200; // Page 45: Iteration limit (assumed value)
        double wMax = 0.9, wMin = 0.2; // Page 27: Nonlinear inertia reduction
        double c1 = 1.5, c2 = 2.5; // Page 45: Cognitive and social coefficients
        double vMax = numVms / 2.0; // Page 27: Velocity limitation

        class Particle {

            int[] position;
            double[] velocity;
            int[] pBestPosition;
            double pBestFitness;
            double fitness;

            Particle() {
                position = new int[numCloudlets];
                velocity = new double[numCloudlets];
                pBestPosition = new int[numCloudlets];
                pBestFitness = Double.MAX_VALUE;
                fitness = Double.MAX_VALUE;
                initialize();
            }

            void initialize() {
                // Page 45: Randomly assign tasks to VMs, balancing load
                int[] vmCounts = new int[numVms];
                for (int i = 0; i < numCloudlets; i++) {
                    int vmIndex;
                    do {
                        vmIndex = random.nextInt(numVms);
                    } while (vmCounts[vmIndex] >= Math.ceil((double) numCloudlets / numVms));
                    position[i] = vmIndex;
                    vmCounts[vmIndex]++;
                    velocity[i] = random.nextDouble() * 0.2 - 0.1;
                }
                System.arraycopy(position, 0, pBestPosition, 0, numCloudlets);
            }

            void updatePosition(double w, Particle gBest) {
                // Page 45: Update velocity and position per PSO equations
                for (int i = 0; i < numCloudlets; i++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();
                    velocity[i] = w * velocity[i]
                            + c1 * r1 * (pBestPosition[i] - position[i])
                            + c2 * r2 * (gBest.position[i] - position[i]);
                    velocity[i] = Math.max(-vMax, Math.min(vMax, velocity[i]));
                    position[i] = Math.abs((position[i] + (int) Math.round(velocity[i])) % numVms);
                }
            }
        }

        List<Particle> swarm = new ArrayList<>();
        Particle gBest = new Particle();
        gBest.fitness = Double.MAX_VALUE;

        for (int i = 0; i < numParticles; i++) {
            swarm.add(new Particle());
        }

        for (int iter = 0; iter < maxIterations; iter++) {
            // Page 27: Nonlinear inertia weight reduction
            double w = wMax - (wMax - wMin) * Math.pow((double) iter / maxIterations, 2);

            for (Particle particle : swarm) {
                // Page 45: Evaluate fitness (response time, load balance, energy)
                particle.fitness = calculateFitness(particle.position, cloudletList, vmList, cloudletCpuRequests, hostList);
                if (particle.fitness < particle.pBestFitness) {
                    particle.pBestFitness = particle.fitness;
                    System.arraycopy(particle.position, 0, particle.pBestPosition, 0, numCloudlets);
                }
                if (particle.fitness < gBest.fitness) {
                    gBest.fitness = particle.fitness;
                    System.arraycopy(particle.position, 0, gBest.position, 0, numCloudlets);
                }
                particle.updatePosition(w, gBest);
            }

            // Page 46: Perform migrations based on host utilization
            performCloudletMigration(cloudletList, vmList, hostList, cloudletCpuRequests, broker, currentTime + iter * 100.0);
        }

        // Page 45: Assign cloudlets to VMs using global best solution
        for (int i = 0; i < numCloudlets; i++) {
            int cloudletId = cloudletList.get(i).getCloudletId();
            int vmId = vmList.get(gBest.position[i]).getId();
            broker.bindCloudletToVm(cloudletId, vmId);
            double submissionTime = cloudletSubmissionTimes.getOrDefault(cloudletId, currentTime);
            cloudletList.get(i).setExecStartTime(submissionTime);
            simulationLogger.logAssignment(cloudletId, vmId, submissionTime);
        }

        // After scheduling and migrations, calculate final host active states
        Map<Integer, Double> hostUtilization = new HashMap<>();
        Map<Integer, Boolean> hostActive = new HashMap<>();
        Map<Integer, List<Cloudlet>> vmCloudlets = new HashMap<>();
        for (Host host : hostList) {
            hostUtilization.put(host.getId(), 0.0);
            hostActive.put(host.getId(), true);
            appendDebugLog(" // After scheduling and migrations, calculate final host active states" + host.getId());

        }
        for (Vm vm : vmList) {
            vmCloudlets.put(vm.getId(), new ArrayList<>());
        }
        for (Cloudlet cloudlet : cloudletList) {
            int vmId = cloudlet.getVmId();
            if (vmId >= 0) {
                vmCloudlets.get(vmId).add(cloudlet);
                int hostId = vmList.stream().filter(v -> v.getId() == vmId).findFirst().get().getHost().getId();
                double cpuUtil = cloudletCpuRequests.get(cloudlet.getCloudletId()) * config.vmMips;
                double hostUtil = cpuUtil / (config.peMips * config.numPesPerHost);
                hostUtilization.put(hostId, hostUtilization.getOrDefault(hostId, 0.0) + hostUtil);
            }
        }

        appendDebugLog("=== EPSO Final Host States ===");
        for (Host host : hostList) {
            int hostId = host.getId();
            double util = hostUtilization.getOrDefault(hostId, 0.0);
            appendDebugLog("Host " + hostId + " utilization: " + util);
            boolean bool = util < 0.15 && !host.getVmList().isEmpty();
            appendDebugLog("eeeece" + bool);
            if (util < 0.15 && !host.getVmList().isEmpty()) {

                // Check if the host is truly idle after migrations
                boolean hasVms = vmCloudlets.entrySet().stream()
                        .filter(e -> vmList.stream().anyMatch(v -> v.getId() == e.getKey() && v.getHost().getId() == hostId))
                        .allMatch(e -> e.getValue().isEmpty());
                appendDebugLog("has vms " + hasVms);
                if (vmCloudlets.entrySet().stream()
                        .filter(e -> vmList.stream().anyMatch(v -> v.getId() == e.getKey() && v.getHost().getId() == hostId))
                        .allMatch(e -> e.getValue().isEmpty())) {
                    hostActive.put(hostId, false);
                    appendDebugLog(" Check if the host is truly idle after migrations " + host.getId() + " = " + hostActive.get(hostId));

                    simulationLogger.logHostPowerOff(hostId, currentTime);
                    appendDebugLog("Host " + hostId + " powered off (util < 0.15 and idle after migrations)");
                } else {
                    appendDebugLog("Host " + hostId + " remains active (has running cloudlets)");
                }
            } else {
                appendDebugLog("Host " + hostId + " remains active (util >= 0.15 or no VMs)");
                hostActive.put(hostId, false);

            }
        }

        return hostActive;
    }

    private void performCloudletMigration(List<Cloudlet> cloudletList, List<Vm> vmList, List<Host> hostList,
            Map<Integer, Double> cloudletCpuRequests, DatacenterBroker broker, double currentTime) {
        // Page 46: Migrate cloudlets from overloaded (>= 1.0) or underloaded (< 0.15) hosts
        Map<Integer, Double> hostUtilization = new HashMap<>();
        Map<Integer, List<Cloudlet>> vmCloudlets = new HashMap<>();
        Map<Integer, Boolean> hostActive = new HashMap<>();
        for (Host host : hostList) {
            hostUtilization.put(host.getId(), 0.0);
            hostActive.put(host.getId(), true);
            appendDebugLog("Host " + host.getId() + " remains active (util >= 0.15 or no VMs)");

        }
        for (Vm vm : vmList) {
            vmCloudlets.put(vm.getId(), new ArrayList<>());
        }
        for (Cloudlet cloudlet : cloudletList) {
            int vmId = cloudlet.getVmId();
            if (vmId >= 0) {
                vmCloudlets.get(vmId).add(cloudlet);
                int hostId = vmList.stream().filter(v -> v.getId() == vmId).findFirst().get().getHost().getId();
                double cpuUtil = cloudletCpuRequests.get(cloudlet.getCloudletId()) * config.vmMips;
                double hostUtil = cpuUtil / (config.peMips * config.numPesPerHost);
                hostUtilization.put(hostId, hostUtilization.getOrDefault(hostId, 0.0) + hostUtil);
            }
        }

        for (Host host : hostList) {
            int hostId = host.getId();
            double util = hostUtilization.getOrDefault(hostId, 0.0);

            if (util >= 1.0) {
                List<Vm> vmsOnHost = host.getVmList();
                while (util >= 1.0 && !vmsOnHost.isEmpty()) {
                    Vm vm = vmsOnHost.get(random.nextInt(vmsOnHost.size()));
                    List<Cloudlet> cloudlets = vmCloudlets.get(vm.getId());
                    if (cloudlets.isEmpty()) {
                        continue;
                    }
                    Cloudlet cloudlet = cloudlets.get(random.nextInt(cloudlets.size()));

                    Host targetHost = null;
                    for (Host h : hostList) {
                        if (hostUtilization.getOrDefault(h.getId(), 0.0) < 0.1 && h.getId() != hostId && hostActive.get(h.getId())) {
                            targetHost = h;
                            break;
                        }
                    }
                    if (targetHost == null) {
                        for (Host h : hostList) {
                            if (hostUtilization.getOrDefault(h.getId(), 0.0) < 0.8 && h.getId() != hostId && hostActive.get(h.getId())) {
                                targetHost = h;
                                break;
                            }
                        }
                    }

                    if (targetHost != null) {
                        Vm targetVm = targetHost.getVmList().get(random.nextInt(targetHost.getVmList().size()));
                        broker.bindCloudletToVm(cloudlet.getCloudletId(), targetVm.getId());
                        simulationLogger.logMigration(cloudlet.getCloudletId(), vm.getId(), targetVm.getId(), currentTime);
                        cloudlet.setExecStartTime(currentTime + MIGRATION_DOWNTIME);

                        double cpuUtil = cloudletCpuRequests.get(cloudlet.getCloudletId()) * config.vmMips;
                        double utilChange = cpuUtil / (config.peMips * config.numPesPerHost);
                        hostUtilization.put(hostId, hostUtilization.get(hostId) - utilChange);
                        hostUtilization.put(targetHost.getId(), hostUtilization.getOrDefault(targetHost.getId(), 0.0) + utilChange);
                        util = hostUtilization.get(hostId);

                        vmCloudlets.get(vm.getId()).remove(cloudlet);
                        vmCloudlets.get(targetVm.getId()).add(cloudlet);
                    } else {
                        break;
                    }
                }
            } else if (util < 0.15 && !host.getVmList().isEmpty()) {
                List<Vm> vmsOnHost = new ArrayList<>(host.getVmList());
                for (Vm vm : vmsOnHost) {
                    List<Cloudlet> cloudlets = new ArrayList<>(vmCloudlets.get(vm.getId()));
                    for (Cloudlet cloudlet : cloudlets) {
                        Host targetHost = null;
                        for (Host h : hostList) {
                            if (hostUtilization.getOrDefault(h.getId(), 0.0) < 0.8 && h.getId() != hostId && hostActive.get(h.getId())) {
                                targetHost = h;
                                break;
                            }
                        }
                        if (targetHost != null) {
                            Vm targetVm = targetHost.getVmList().get(random.nextInt(targetHost.getVmList().size()));
                            broker.bindCloudletToVm(cloudlet.getCloudletId(), targetVm.getId());
                            simulationLogger.logMigration(cloudlet.getCloudletId(), vm.getId(), targetVm.getId(), currentTime);
                            cloudlet.setExecStartTime(currentTime + MIGRATION_DOWNTIME);

                            double cpuUtil = cloudletCpuRequests.get(cloudlet.getCloudletId()) * config.vmMips;
                            double utilChange = cpuUtil / (config.peMips * config.numPesPerHost);
                            hostUtilization.put(hostId, hostUtilization.get(hostId) - utilChange);
                            hostUtilization.put(targetHost.getId(), hostUtilization.getOrDefault(targetHost.getId(), 0.0) + utilChange);

                            vmCloudlets.get(vm.getId()).remove(cloudlet);
                            vmCloudlets.get(targetVm.getId()).add(cloudlet);
                        }
                    }
                }
                // Page 46: Power off idle hosts
                if (vmCloudlets.entrySet().stream()
                        .filter(e -> vmList.stream().anyMatch(v -> v.getId() == e.getKey() && v.getHost().getId() == hostId))
                        .allMatch(e -> e.getValue().isEmpty())) {
                    hostActive.put(hostId, false);
                    appendDebugLog("Power off host  " + hostId + " due to low utilization");

                    simulationLogger.logHostPowerOff(hostId, currentTime);
                }
            }
        }
    }

    private double calculateFitness(int[] mapping, List<Cloudlet> cloudletList, List<Vm> vmList,
            Map<Integer, Double> cloudletCpuRequests, List<Host> hostList) {
        appendDebugLog("=== Calculating Fitness (Energy Component) ===");
        // Page 45: Fitness evaluates response time, load balance, and energy
        // Response time
        Map<Integer, Double> vmLoad = new HashMap<>();
        for (Vm vm : vmList) {
            vmLoad.put(vm.getId(), 0.0);
        }
        for (int i = 0; i < cloudletList.size(); i++) {
            Cloudlet cloudlet = cloudletList.get(i);
            int vmId = vmList.get(mapping[i]).getId();
            double cloudletLength = cloudlet.getCloudletLength();
            double vmMips = vmList.get(mapping[i]).getMips();
            double execTime = cloudletLength / vmMips;
            vmLoad.put(vmId, vmLoad.get(vmId) + execTime);
        }
        double maxResponseTime = vmLoad.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double avgLoad = vmLoad.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double loadVariance = vmLoad.values().stream().mapToDouble(load -> Math.pow(load - avgLoad, 2)).average().orElse(0.0);
        appendDebugLog("Max Response Time: " + maxResponseTime);
        appendDebugLog("Load Variance: " + loadVariance);

        // Energy with power-off simulation
        Map<Integer, Double> hostUtilization = new HashMap<>();
        Map<Integer, Boolean> hostActive = new HashMap<>();
        Map<Integer, List<Cloudlet>> vmCloudlets = new HashMap<>();
        int migrationCount = 0;
        for (Host host : hostList) {
            hostUtilization.put(host.getId(), 0.0);
            hostActive.put(host.getId(), true);
            appendDebugLog("Hostjhjcw " + hostActive.get(host.getId()) + " ");

        }
        for (Vm vm : vmList) {
            vmCloudlets.put(vm.getId(), new ArrayList<>());
        }
        for (int i = 0; i < cloudletList.size(); i++) {
            int vmId = vmList.get(mapping[i]).getId();
            vmCloudlets.get(vmId).add(cloudletList.get(i));
            int hostId = vmList.stream().filter(v -> v.getId() == vmId).findFirst().get().getHost().getId();
            double cpuUtil = cloudletCpuRequests.get(cloudletList.get(i).getCloudletId()) * config.vmMips;
            double hostUtil = cpuUtil / (config.peMips * config.numPesPerHost);
            hostUtilization.put(hostId, hostUtilization.get(hostId) + hostUtil);
        }
        double totalEnergy = 0.0;
        for (Host host : hostList) {
            int hostId = host.getId();
            double avgUtil = Math.min(hostUtilization.get(hostId), 1.0);
            appendDebugLog("Host " + hostId + " utilization: " + avgUtil);
            // Simplified power-off condition: power off if utilization is below threshold
            if (avgUtil < 0.15) {
                avgUtil = 0.0;
                hostActive.put(hostId, false);
                appendDebugLog(" 1  yehey: Host " + hostId + " powered off in fitness calc (util < 0.15)");
                appendDebugLog("yehey: Host " + hostId + hostActive.get(hostId) + " evwe" + hostActive.toString());

                int vmId = vmList.stream().filter(v -> v.getHost().getId() == hostId).findFirst().map(Vm::getId).orElse(-1);
                if (vmId != -1) {
                    int migrations = vmCloudlets.getOrDefault(vmId, new ArrayList<>()).size();
                    migrationCount += migrations;
                    appendDebugLog("Host " + hostId + " caused " + migrations + " migrations");
                }
            } else {
                appendDebugLog("Host " + hostId + " remains active in fitness calc");
            }
            // Page 42: Power model for energy calculation
            double power = hostActive.get(hostId) && avgUtil > 0 ? (POWER_BUSY - POWER_IDLE) * avgUtil + POWER_IDLE : 0.0;
            double simulationTime = 5538.46 / 3600.0; // Page 42: Simulated duration
            double energy = power * simulationTime;
            appendDebugLog("Host " + hostId + " power: " + power + " W, energy: " + energy + " Wh (simTime: " + simulationTime + " hr)");
            totalEnergy += energy;
        }
        totalEnergy += migrationCount * MIGRATION_ENERGY_COST;
        appendDebugLog("Total migration energy: " + (migrationCount * MIGRATION_ENERGY_COST) + " Wh (migration count: " + migrationCount + ")");
        appendDebugLog("Total energy in fitness: " + totalEnergy + " Wh");

        // Page 45: Weighted fitness (weights assumed for balanced optimization)
        double wEnergy = 0.7; // Prioritizes energy efficiency
        double wResponse = 0.3; // Balances response time
        double wLoad = 0.1; // Ensures load distribution
        double fitness = wEnergy * totalEnergy + wResponse * maxResponseTime + wLoad * loadVariance;
        appendDebugLog("Fitness breakdown: energy term = " + (wEnergy * totalEnergy) + ", response term = " + (wResponse * maxResponseTime) + ", load term = " + (wLoad * loadVariance));
        appendDebugLog("Final fitness: " + fitness);
        return fitness;
    }

    private void logFinalResults(List<Cloudlet> cloudletList, List<Vm> vmList,
            Map<Integer, Double> cloudletCpuRequests, List<Host> hostList, Map<Integer, Boolean> hostActive) {
        appendDebugLog("=== Logging Final Energy Results ===");
        Map<Integer, Double> hostUtilization = new HashMap<>();
        Map<Integer, List<Cloudlet>> vmCloudlets = new HashMap<>();
        int migrationCount = 0;
        for (Host host : hostList) {
            hostUtilization.put(host.getId(), 0.0);
        }
        for (Vm vm : vmList) {
            vmCloudlets.put(vm.getId(), new ArrayList<>());
        }
        for (Cloudlet cloudlet : cloudletList) {
            int vmId = cloudlet.getVmId();
            if (vmId >= 0) {
                vmCloudlets.get(vmId).add(cloudlet);
                int hostId = vmList.stream().filter(v -> v.getId() == vmId).findFirst().get().getHost().getId();
                double cpuUtil = cloudletCpuRequests.get(cloudlet.getCloudletId()) * config.vmMips;
                double hostUtil = cpuUtil / (config.peMips * config.numPesPerHost);
                hostUtilization.put(hostId, hostUtilization.getOrDefault(hostId, 0.0) + hostUtil);
            }
        }

        double totalEnergy = 0.0;
        List<Map<String, Object>> hostMetrics = new ArrayList<>();
        for (Host host : hostList) {
            int hostId = host.getId();
            double avgUtil = Math.min(hostUtilization.getOrDefault(hostId, 0.0), 1.0);
            appendDebugLog("Host " + hostId + " final utilization: " + avgUtil);
            appendDebugLog("Host " + hostId + " active state: " + hostActive.get(hostId));
            double power = hostActive.get(hostId) && avgUtil > 0 ? (POWER_BUSY - POWER_IDLE) * avgUtil + POWER_IDLE : 0.0;
            double simulationTime = 100.0 / 3600.0;
            double energy = power * simulationTime;
            appendDebugLog("Host " + hostId + " power: " + power + " W, energy: " + energy + " Wh (simTime: " + simulationTime + " hr)");
            totalEnergy += energy;

            hostMetrics.add(simulationLogger.createHostMetrics(hostId, avgUtil, power, energy));
        }
        totalEnergy += migrationCount * MIGRATION_ENERGY_COST;
        appendDebugLog("Total migration energy (final): " + (migrationCount * MIGRATION_ENERGY_COST) + " Wh (migration count: " + migrationCount + ")");
        appendDebugLog("Total energy (final): " + totalEnergy + " Wh");

        simulationLogger.logSimulationResults(totalEnergy, hostMetrics);
    }
}
