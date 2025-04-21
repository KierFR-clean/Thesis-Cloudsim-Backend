package CloudSimTestRR;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Vm;

public class VmFactory {
    private SimulationConfig config;

    public VmFactory(SimulationConfig config) {
        this.config = config;
    }

    public List<Vm> createVMs(int brokerId) {
        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < config.numVms; i++) {
            Vm vm = new Vm(i, brokerId, config.vmMips, config.vmPes, config.vmRam, config.vmBw, config.vmSize,
                    "Xen", config.vmScheduler.equals("SpaceShared") ?
                    new CloudletSchedulerSpaceShared() : new CloudletSchedulerTimeShared());
            vms.add(vm);
        }
        return vms;
    }
}