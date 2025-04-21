package CloudSimTestRR;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend
public class SimulationController {

    private String simulationResults = "{}";
    private String schedulingLog = "[]"; // Store structured scheduling log

    @PostMapping("/run")
    public ResponseEntity<String> runSimulation(@RequestBody SimulationConfig config) {
        CSVTaskSimulation simulation = new CSVTaskSimulation(config);

        try {
            System.out.println("Starting CloudSim simulation...");
            simulationResults = simulation.runSimulation();
            schedulingLog = simulation.getSchedulingLog(); // Capture structured scheduling log
            System.out.println("CloudSim simulation completed.");
            System.out.println("Energy Results" + simulation.getEnergyResults());
            
            return ResponseEntity.ok(simulationResults);

        } catch (Exception e) {
            System.err.println("Error running simulation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error running simulation");
        }
    }

    @GetMapping("/results")
    public ResponseEntity<String> getSimulationResults() {
        return ResponseEntity.ok(simulationResults);
    }

    @GetMapping(value = "/logs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSchedulingLog() {
        return ResponseEntity.ok(schedulingLog);
    }

    @PostMapping("/run-with-file")
    public ResponseEntity<String> runSimulationWithFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> params) {
        
        try {
            // Convert params to SimulationConfig
            SimulationConfig config = new SimulationConfig();
            config.numHosts = Integer.parseInt(params.getOrDefault("numHosts", "10"));
            config.numVms = Integer.parseInt(params.getOrDefault("numVMs", "20"));
            config.numPesPerHost = Integer.parseInt(params.getOrDefault("numPesPerHost", "4"));
            config.peMips = Integer.parseInt(params.getOrDefault("peMips", "1000"));
            config.optimizationAlgorithm = params.getOrDefault("optimizationAlgorithm", "RoundRobin");
            // Set other fields as needed
            
            // Save uploaded file to a temp location
            String tempFilePath = System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename();
            file.transferTo(new java.io.File(tempFilePath));
            
            // Set the CSV file path in the config
            config.csvFilePath = tempFilePath;
            
            // Run simulation with uploaded file
            CSVTaskSimulation simulation = new CSVTaskSimulation(config);
            simulationResults = simulation.runSimulation();
            schedulingLog = simulation.getSchedulingLog();
            
            return ResponseEntity.ok(simulationResults);
        } catch (Exception e) {
            System.err.println("Error running simulation with file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}