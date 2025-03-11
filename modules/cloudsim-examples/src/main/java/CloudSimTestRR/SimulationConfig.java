package CloudSimTestRR;

public class SimulationConfig {
    // Data center configuration
    public int numHosts;          // Number of hosts in the data center
    public int numPesPerHost;     // Number of Processing Elements (PEs) per host
    public int peMips;            // MIPS capacity of each PE
    public int ramPerHost;        // RAM capacity of each host (in MB)
    public int bwPerHost;         // Bandwidth capacity of each host (in Mbps)
    public int storagePerHost;    // Storage capacity of each host (in MB)

    // VM configuration
    public int numVms;            // Number of VMs to create
    public int vmMips;            // MIPS capacity of each VM
    public int vmPes;             // Number of PEs for each VM
    public int vmRam;             // RAM capacity of each VM (in MB)
    public int vmBw;              // Bandwidth capacity of each VM (in Mbps)
    public int vmSize;            // Storage capacity of each VM (in MB)
    public String vmScheduler;    // VM scheduler type (e.g., "TimeShared")

    // Cloudlet configuration
    public int numCloudlets;      // Number of cloudlets to create
    public long cloudletLength;   // Length of each cloudlet (in MI)
    public int cloudletPes;       // Number of PEs required by each cloudlet
    public String cloudletExecType; // Execution type: "Fixed" or "Random"

    // Workload configuration
    public String workloadType;   // Workload type: "Even" or "Uneven"
}