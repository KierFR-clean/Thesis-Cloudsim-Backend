package CloudSimTestRR;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;

import java.util.*;

public class LoadBalancingSimulation {
    public static void main(String[] args) {
        System.out.println("Starting Load Balancing Simulation with RR and PSO...");
        try {
            Random random = new Random(); // Set fixed seed for deterministic behavior
            CloudSim.init(1, Calendar.getInstance(), false);
            Datacenter datacenter = createDatacenter("Datacenter_1");
            DatacenterBroker brokerRR = createBroker("Broker_RR");
            DatacenterBroker brokerPSO = createBroker("Broker_PSO");

            List<Vm> vmListRR = createVms(brokerRR.getId());
            List<Vm> vmListPSO = createVms(brokerPSO.getId());
            brokerRR.submitGuestList(vmListRR);
            brokerPSO.submitGuestList(vmListPSO);

            List<Cloudlet> cloudletListRR = createCloudlets(brokerRR.getId());
            List<Cloudlet> cloudletListPSO = createCloudlets(brokerPSO.getId());
            brokerRR.submitCloudletList(cloudletListRR);
            brokerPSO.submitCloudletList(cloudletListPSO);

            // Round Robin Load Balancing
            System.out.println("\n--- Running Round Robin Load Balancing ---");
            bindCloudletsToVmsRoundRobin(brokerRR, cloudletListRR, vmListRR);

            // PSO-Based Load Balancing
            System.out.println("\n--- Running PSO-Based Load Balancing ---");
            bindCloudletsToVmsPSO(brokerPSO, cloudletListPSO, vmListPSO, random);

            CloudSim.startSimulation();

            // Collect and print results
            List<Cloudlet> cloudletsRR = brokerRR.getCloudletReceivedList();
            List<Cloudlet> cloudletsPSO = brokerPSO.getCloudletReceivedList();

            printCloudletResults("Round Robin", cloudletsRR);
            printCloudletResults("PSO", cloudletsPSO);

            CloudSim.stopSimulation();

            System.out.println("Simulation completed!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Simulation terminated due to an error.");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(4000)));
        }
        hostList.add(new Host(0, new RamProvisionerSimple(8192), new BwProvisionerSimple(100000),
                1000000, peList, new VmSchedulerTimeShared(peList)));

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

    private static List<Vm> createVms(int brokerId) {
        List<Vm> vmList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            vmList.add(new Vm(i, brokerId, 1000, 1, 1024, 2000, 20000, "Xen", new CloudletSchedulerTimeShared()));
        }
        return vmList;
    }

    private static List<Cloudlet> createCloudlets(int brokerId) {
        List<Cloudlet> cloudletList = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < 8; i++) {
            // Uneven workloads: Cloudlet length varies significantly
            long length = 20000 + rand.nextInt(80000); // Workload range: 20K - 100K
            int pesNumber = 1; // Single processing element per cloudlet
            long fileSize = 300;
            long outputSize = 300;

            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize,
                    new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }
        return cloudletList;
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

    private static void bindCloudletsToVmsPSO(DatacenterBroker broker, List<Cloudlet> cloudletList, List<Vm> vmList, Random rand) {
        // PSO Parameters
        int numParticles = 10; // Number of particles
        int maxIterations = 100; // Maximum iterations
        double inertiaWeight = 0.729; // Inertia weight
        double cognitiveWeight = 1.49445; // Cognitive weight
        double socialWeight = 1.49445; // Social weight

        // Initialize particles
        List<Particle> particles = new ArrayList<>();
        for (int i = 0; i < numParticles; i++) {
            particles.add(new Particle(cloudletList.size(), vmList.size(), rand));
        }

        // PSO Algorithm
        Particle globalBestParticle = particles.get(0);
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            for (Particle particle : particles) {
                // Update velocity and position
                particle.updateVelocity(inertiaWeight, cognitiveWeight, socialWeight, globalBestParticle, rand);
                particle.updatePosition(vmList.size()); // Ensure position is within bounds

                // Evaluate fitness
                double fitness = particle.calculateFitness();
                if (fitness < globalBestParticle.getBestFitness()) {
                    globalBestParticle = particle; // Update global best
                }
            }
        }

        // Assign cloudlets to VMs based on the best solution found
        int[] bestSolution = globalBestParticle.getPosition();
        for (int i = 0; i < cloudletList.size(); i++) {
            Vm assignedVm = vmList.get(bestSolution[i]);
            broker.bindCloudletToVm(cloudletList.get(i).getCloudletId(), assignedVm.getId());
            System.out.println("Cloudlet " + cloudletList.get(i).getCloudletId() + " assigned to VM " + assignedVm.getId() + " using PSO");
        }
    }

    private static void printCloudletResults(String method, List<Cloudlet> cloudletList) {
        System.out.println("\nResults for " + method + " Load Balancing");
        System.out.println("Cloudlet ID\tStatus\tVM ID\tTime\tStart Time\tFinish Time");
        for (Cloudlet cloudlet : cloudletList) {
            System.out.printf("%d\t\t%s\t%d\t%.2f\t%.2f\t\t%.2f\n",
                    cloudlet.getCloudletId(), cloudlet.getCloudletStatusString(), cloudlet.getGuestId(),
                    cloudlet.getActualCPUTime(), cloudlet.getExecStartTime(), cloudlet.getFinishTime());
        }
    }

    // Particle class for PSO
    private static class Particle {
        private int[] position; // Represents a solution (mapping of cloudlets to VMs)
        private int[] velocity; // Velocity for updating position
        private int[] bestPosition; // Best position found by this particle
        private double bestFitness; // Best fitness value found by this particle

        public Particle(int numCloudlets, int numVms, Random rand) {
            position = new int[numCloudlets];
            velocity = new int[numCloudlets];
            bestPosition = new int[numCloudlets];
            for (int i = 0; i < numCloudlets; i++) {
                position[i] = rand.nextInt(numVms); // Random initial position
                velocity[i] = rand.nextInt(numVms); // Random initial velocity
                bestPosition[i] = position[i];
            }
            bestFitness = calculateFitness(); // Evaluate initial fitness
        }

        public int[] getPosition() {
            return position;
        }

        public double getBestFitness() {
            return bestFitness;
        }

        public void updateVelocity(double inertiaWeight, double cognitiveWeight, double socialWeight, Particle globalBest, Random rand) {
            for (int i = 0; i < position.length; i++) {
                // Update velocity using PSO formula
                velocity[i] = (int) (inertiaWeight * velocity[i] +
                        cognitiveWeight * rand.nextDouble() * (bestPosition[i] - position[i]) +
                        socialWeight * rand.nextDouble() * (globalBest.getPosition()[i] - position[i]));

                // Limit velocity to prevent extreme values
                velocity[i] = Math.max(-10, Math.min(10, velocity[i]));
            }
        }

        public void updatePosition(int numVms) {
            for (int i = 0; i < position.length; i++) {
                // Update position and ensure it stays within valid bounds
                position[i] = position[i] + velocity[i];
                position[i] = Math.max(0, Math.min(numVms - 1, position[i])); // Bound to [0, numVms - 1]
            }
        }

        public double calculateFitness() {
            // Fitness function: Minimize the standard deviation of VM loads
            Map<Integer, Integer> loadMap = new HashMap<>();
            for (int vmId : position) {
                loadMap.put(vmId, loadMap.getOrDefault(vmId, 0) + 1);
            }
            double mean = (double) position.length / loadMap.size();
            double variance = 0;
            for (int load : loadMap.values()) {
                variance += Math.pow(load - mean, 2);
            }
            double stdDev = Math.sqrt(variance / loadMap.size());
            return stdDev; // Lower stdDev means better load balancing
        }
    }
}