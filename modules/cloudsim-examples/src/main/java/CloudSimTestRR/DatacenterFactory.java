package CloudSimTestRR;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class DatacenterFactory {

    private SimulationConfig config;

    public DatacenterFactory(SimulationConfig config) {
        this.config = config;
    }

    public Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();
        for (int h = 0; h < config.numHosts; h++) {
            List<Pe> peList = new ArrayList<>();
            for (int i = 0; i < config.numPesPerHost; i++) {
                peList.add(new Pe(i, new PeProvisionerSimple(config.peMips)));
            }
            Host host = new Host(h, new RamProvisionerSimple(config.ramPerHost), new BwProvisionerSimple(config.bwPerHost), config.storagePerHost, peList, new VmSchedulerTimeShared(peList));
            hostList.add(host);
        }

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        return new Datacenter(name, characteristics,
                new VmAllocationPolicySimple(hostList),
                new LinkedList<Storage>(), 0);
    }

    public DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("CSVTaskBroker");
    }
}
