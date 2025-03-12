package CloudSimTestRR;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")// Allow React frontend
public class SimulationController {

    private String simulationResults = "{}";
    private String simulationLogs = ""; // Store simulation logs

    @PostMapping("/run")
    public ResponseEntity<String> runSimulation(@RequestBody SimulationConfig config) {
        RoundRobinSimulation simulation = new RoundRobinSimulation(config);

        try {
            System.out.println("Starting CloudSim simulation...");
            simulationResults = simulation.runSimulation();
            simulationLogs = simulation.getLogs(); // Capture logs
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

    @GetMapping("/logs")
    public ResponseEntity<String> getSimulationLogs() {
        return ResponseEntity.ok(simulationLogs); // Return simulation logs
    }
}
