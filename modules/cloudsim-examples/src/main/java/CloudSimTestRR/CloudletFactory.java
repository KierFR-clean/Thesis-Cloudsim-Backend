package CloudSimTestRR;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CloudletFactory {
    private SimulationConfig config;
    private Map<Integer, Double> cloudletSubmissionTimes;
    private Map<Integer, Double> cloudletCpuRequests;

    public CloudletFactory(SimulationConfig config, Map<Integer, Double> cloudletSubmissionTimes,
                           Map<Integer, Double> cloudletCpuRequests) {
        this.config = config;
        this.cloudletSubmissionTimes = cloudletSubmissionTimes;
        this.cloudletCpuRequests = cloudletCpuRequests;
    }

    public List<Cloudlet> readTasksFromCSV(int brokerId) throws Exception {
        List<Cloudlet> cloudlets = new ArrayList<>();
        if (config.workloadType.equals("Synthetic")) {
            for (int count = 0; count < config.numCloudlets; count++) {
                double cpuRequest = 0.05;
                UtilizationModel cpuUtilizationModel = new UtilizationModel() {
                    @Override
                    public double getUtilization(double time) {
                        return cpuRequest;
                    }
                };
                Cloudlet cloudlet = new Cloudlet(count, config.cloudletLength, config.cloudletPes,
                        1000000, 1000000, cpuUtilizationModel, new UtilizationModelFull(),
                        new UtilizationModelFull());
                cloudlet.setUserId(brokerId);
                cloudlets.add(cloudlet);
                cloudletSubmissionTimes.put(count, count * 50.0);
                cloudletCpuRequests.put(count, cpuRequest);
            }
            return cloudlets;
        }

        String csvFile = "C:\\Users\\reyes\\Downloads\\python_file\\cloudsim_task_events_part00000_preprocessed.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = br.readLine();
            int count = 0;
            double firstTimestamp = -1;

            while ((line = br.readLine()) != null && count < config.numCloudlets) {
                String[] data = line.split(",");
                if (data.length < 9) {
                    throw new IllegalArgumentException("Malformed CSV: Missing columns in row " + (count + 2));
                }

                double cpuRequest;
                try {
                    cpuRequest = Double.parseDouble(data[4]);
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid cpu_request at line " + (count + 2) + ": " + data[4], e);
                }
                long length = (long) (cpuRequest * 1000000);

                double memoryRequest;
                try {
                    memoryRequest = Double.parseDouble(data[5]);
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid memory_request at line " + (count + 2) + ": " + data[5], e);
                }
                long fileSize = (long) (memoryRequest * 1000000);

                double diskSpaceRequest;
                try {
                    diskSpaceRequest = Double.parseDouble(data[6]);
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid disk_space_request at line " + (count + 2) + ": " + data[6], e);
                }
                long outputSize = (long) (diskSpaceRequest * 1000000);

                double unixTimestamp;
                try {
                    unixTimestamp = Double.parseDouble(data[7]);
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid time_seconds at line " + (count + 2) + ": " + data[7], e);
                }
                if (firstTimestamp == -1) {
                    firstTimestamp = unixTimestamp;
                }
                double relativeTime = unixTimestamp - firstTimestamp;

                UtilizationModel cpuUtilizationModel = new UtilizationModel() {
                    @Override
                    public double getUtilization(double time) {
                        return cpuRequest;
                    }
                };

                Cloudlet cloudlet = new Cloudlet(count, length, config.cloudletPes, fileSize, outputSize,
                        cpuUtilizationModel, new UtilizationModelFull(),
                        new UtilizationModelFull());
                cloudlet.setUserId(brokerId);
                cloudlets.add(cloudlet);

                cloudletSubmissionTimes.put(count, relativeTime);
                cloudletCpuRequests.put(count, cpuRequest);
                count++;
            }
        } catch (java.io.FileNotFoundException e) {
            throw new Exception("CSV file not found: " + csvFile, e);
        }
        return cloudlets;
    }
}