package CloudSimTestRR;

public class SimulationConfig {
    // Data center configuration
    public int numHosts;          // Number of hosts in the data center
    public int numPesPerHost;     // Number of Processing Elements (PEs) per host
    public int peMips;            // MIPS capacity of each PE
    public int ramPerHost;        // RAM capacity of each host (in MB)
    public long bwPerHost;        // Bandwidth capacity of each host (in Mbps)
    public long storagePerHost;    // Storage capacity of each host (in MB)

    // VM configuration
    public int numVms;            // Number of VMs to create
    public int vmMips;            // MIPS capacity of each VM
    public int vmPes;             // Number of PEs for each VM
    public int vmRam;             // RAM capacity of each VM (in MB)
    public long vmBw;             // Bandwidth capacity of each VM (in Mbps)
    public long vmSize;           // Storage capacity of each VM (in MB)
    public String vmScheduler;    // VM scheduler type (e.g., "TimeShared", "SpaceShared")

    // Cloudlet configuration
    public int numCloudlets;      // Number of cloudlets to create
    public long cloudletLength;   // Length of each cloudlet (in MI)
    public int cloudletPes;       // Number of PEs required by each cloudlet

    // Workload configuration
    public String workloadType;   // Workload type: "CSV" or "Synthetic"
    public String csvFilePath;    // Path to CSV file for workload

    // Optimization configuration
    public String optimizationAlgorithm; // Scheduling algorithm: "RoundRobin" or "EPSO"

    // Constructor with defaults
    public SimulationConfig() {
        this.numHosts = 10;
        this.numPesPerHost = 2;
        this.peMips = 2000;
        this.ramPerHost = 2048;
        this.bwPerHost = 10000;
        this.storagePerHost = 100000;
        this.numVms = 10;
        this.vmMips = 1000;
        this.vmPes = 2;
        this.vmRam = 1024;
        this.vmBw = 1000;
        this.vmSize = 10000;
        this.vmScheduler = "TimeShared";
        this.numCloudlets = 100;
        this.cloudletLength = 40000;
        this.cloudletPes = 1;
        this.workloadType = "CSV";
        this.optimizationAlgorithm = "RoundRobin"; // Default at first 
    }
}