


# Main features #

  * Support for modeling and simulation of large scale Cloud Computing data centers
  * Support for modeling and simulation of virtualized server hosts, with customizable policies for provisioning host resources to Virtual Machines
  * Support for modeling and simulation of application containers
  * Support for modeling and simulation of energy-aware computational resources
  * Support for modeling and simulation of data center network topologies and message-passing applications
  * Support for modeling and simulation of federated clouds
  * Support for dynamic insertion of simulation elements, stop and resume of simulation
  * Support for user-defined policies for allocation of hosts to Virtual Machines and policies for allocation of host resources to Virtual Machines


# Download #

Either clone the repository or download a [release](https://github.com/Cloudslab/cloudsim/releases). The release package contains all the source code, examples, jars, and API html files.

# Installation
**Windows**
1) Install Java JDK21 on your system from the [official website](https://www.oracle.com/in/java/technologies/downloads/#java21) as shown in [JDK installation instructions](https://docs.oracle.com/en/java/javase/23/install/overview-jdk-installation.html)
2) Install Maven as shown on the [official website](https://maven.apache.org/install.html)
4) Compile and Run tests using the command prompt:
  ```prompt
  mvn clean package
  ```
5) Run an example (e.g., CloudSimExample1) in cloudsim-examples using the command prompt:
```prompt
mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.CloudSimExample1
```

**Linux**
  1) Install Java JDK21 on your system:
  - On Debian-based Linux & Windows WSL2: 
    ```bash
    sudo apt install openjdk-21-jdk
    ```
  - On Red Hat-based Linux:  
    ```bash  
    sudo yum install java-21-openjdk
    ```
  2) Set Java JDK21 as default: 
  - On Debian-based Linux & Windows WSL2:
    ```bash
    sudo update-java-alternatives --set java-1.21.0-openjdk-amd64
    ```
  - On Red Hat-based Linux: 
    ```bash
    sudo update-alternatives --config 'java'
    ```
  3) Install Maven as shown on the [Official Website](https://maven.apache.org/install.html)
  4) Compile and run tests using the terminal:
  ```bash
  mvn clean package
  ```
  5) Run an example (e.g., CloudSimExample1) in cloudsim-examples using the terminal:
  ```bash
  mvn exec:java -pl modules/cloudsim-examples/ -Dexec.mainClass=org.cloudbus.cloudsim.examples.CloudSimExample1
  ```

  **Suggestion:** Use an IDE such as IDEA Intellij to faciliate steps 4) and 5)

# CloudSimTestRR Backend Installation #

## Prerequisites
* Java Development Kit (JDK) 21 or later
* Apache Maven (latest stable version)
* Git

## Installation Steps

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd thesis-backend
   ```

2. **Configure Java Environment**
   * Ensure JDK 21 is installed and properly configured
   * Set JAVA_HOME environment variable:
     ```bash
     # Windows (PowerShell)
     set JAVA_HOME=C:\Path\To\Your\JDK21

     # Windows (CMD)
     setx JAVA_HOME "C:\Path\To\Your\JDK21"
     # After setting JAVA_HOME, close and reopen CMD

     # Linux/Mac
     export JAVA_HOME=/path/to/your/jdk21
     ```
   * Add Java to PATH:
     ```bash
     # Windows (PowerShell)
     set PATH=%JAVA_HOME%\bin;%PATH%

     # Windows (CMD)
     setx PATH "%JAVA_HOME%\bin;%PATH%"
     # After setting PATH, close and reopen CMD

     # Linux/Mac
     export PATH=$JAVA_HOME/bin:$PATH
     ```

3. **Configure Maven**
   * For Windows users, you can also set Maven environment variables:
     ```bash
     # Windows (CMD)
     setx M2_HOME "C:\Path\To\Maven"
     setx PATH "%PATH%;%M2_HOME%\bin"
     # Close and reopen CMD after setting variables
     ```
   * Verify Maven installation:
     ```bash
     mvn -version
     ```
   * Should show Java 21 as the Java version

4. **Build the Project**
   ```bash
   # All platforms
   cd thesis-backend
   mvn clean install
   ```

5. **Run the Application**
   ```bash
   # All platforms
   mvn spring-boot:run -pl modules/cloudsim-examples
   ```

## Alternative Installation Using Windows System Properties
1. Open System Properties:
   * Right-click on 'This PC' or 'My Computer'
   * Click 'Properties'
   * Click 'Advanced system settings'
   * Click 'Environment Variables'

2. Set JAVA_HOME:
   * Under 'System variables', click 'New'
   * Variable name: `JAVA_HOME`
   * Variable value: `C:\Path\To\Your\JDK21`
   * Click 'OK'

3. Update PATH:
   * Under 'System variables', find and select 'Path'
   * Click 'Edit'
   * Click 'New'
   * Add `%JAVA_HOME%\bin`
   * Click 'OK' on all windows

4. Set Maven Variables:
   * Add new System variable `M2_HOME`: `C:\Path\To\Maven`
   * Edit 'Path' again and add `%M2_HOME%\bin`
   * Click 'OK' on all windows

5. Verify Installation:
   * Open a new Command Prompt
   * Run:
     ```cmd
     java -version
     mvn -version
     ```

## Verification
* The server should start on port 8080
* Test the API endpoints:
  * GET http://localhost:8080/api/results
  * GET http://localhost:8080/api/logs

## Troubleshooting
* If port 8080 is in use, modify the port in application properties
* Ensure all Maven dependencies are downloaded successfully
* Check Java version compatibility with `java -version`
* For build errors, try:
  ```bash
  mvn clean install -DskipTests
  ```

## API Endpoints
* POST `/api/run` - Run simulation with configuration
* POST `/api/run-with-file` - Run simulation with file upload
* GET `/api/results` - Get simulation results
* GET `/api/logs` - Get scheduling logs

# Preferred Publication #
  * Remo Andreoli, Jie Zhao, Tommaso Cucinotta, and Rajkumar Buyya, [CloudSim 7G: An Integrated Toolkit for Modeling and Simulation of Future Generation Cloud Computing Environments](https://onlinelibrary.wiley.com/doi/10.1002/spe.3413), Software: Practice and Experience, 2025.
    
# Publications (Legacy) #

  * Anton Beloglazov, and Rajkumar Buyya, [Optimal Online Deterministic Algorithms and Adaptive Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in Cloud Data Centers](http://beloglazov.info/papers/2012-optimal-algorithms-ccpe.pdf), Concurrency and Computation: Practice and Experience, Volume 24, Number 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012.
  * Saurabh Kumar Garg and Rajkumar Buyya, [NetworkCloudSim: Modelling Parallel Applications in Cloud Simulations](http://www.cloudbus.org/papers/NetworkCloudSim2011.pdf), Proceedings of the 4th IEEE/ACM International Conference on Utility and Cloud Computing (UCC 2011, IEEE CS Press, USA), Melbourne, Australia, December 5-7, 2011.
  * **Rodrigo N. Calheiros, Rajiv Ranjan, Anton Beloglazov, Cesar A. F. De Rose, and Rajkumar Buyya, [CloudSim: A Toolkit for Modeling and Simulation of Cloud Computing Environments and Evaluation of Resource Provisioning Algorithms](http://www.buyya.com/papers/CloudSim2010.pdf), Software: Practice and Experience (SPE), Volume 41, Number 1, Pages: 23-50, ISSN: 0038-0644, Wiley Press, New York, USA, January, 2011. (Seminal paper)**
  * Bhathiya Wickremasinghe, Rodrigo N. Calheiros, Rajkumar Buyya, [CloudAnalyst: A CloudSim-based Visual Modeller for Analysing Cloud Computing Environments and Applications](http://www.cloudbus.org/papers/CloudAnalyst-AINA2010.pdf), Proceedings of the 24th International Conference on Advanced Information Networking and Applications (AINA 2010), Perth, Australia, April 20-23, 2010.
  * Rajkumar Buyya, Rajiv Ranjan and Rodrigo N. Calheiros, [Modeling and Simulation of Scalable Cloud Computing Environments and the CloudSim Toolkit: Challenges and Opportunities](http://www.cloudbus.org/papers/CloudSim-HPCS2009.pdf), Proceedings of the 7th High Performance Computing and Simulation Conference (HPCS 2009, ISBN: 978-1-4244-4907-1, IEEE Press, New York, USA), Leipzig, Germany, June 21-24, 2009.




[![](http://www.cloudbus.org/logo/cloudbuslogo-v5a.png)](http://cloudbus.org/)

# GitHub Deployment Guide

## Prerequisites
* Git installed on your machine
* GitHub account
* Repository access/permissions

## First-Time Setup
1. **Initialize Git Repository** (if not already initialized)
   ```bash
   cd thesis-backend
   git init
   ```

2. **Configure Git User Information**
   ```bash
   git config --global user.name "Your Name"
   git config --global user.email "your.email@example.com"
   ```

3. **Add Remote Repository**
   ```bash
   git remote add origin https://github.com/username/repository-name.git
   ```

## Pushing Changes to GitHub

1. **Check Repository Status**
   ```bash
   git status
   ```

2. **Add Files to Staging**
   ```bash
   # Add all files
   git add .

   # Or add specific files
   git add pom.xml
   git add modules/
   git add README.md
   ```

3. **Commit Changes**
   ```bash
   git commit -m "Your commit message describing the changes"
   ```

4. **Push to GitHub**
   ```bash
   # First time push
   git push -u origin main

   # Subsequent pushes
   git push
   ```

## Common Git Operations

* **Create New Branch**
  ```bash
  git checkout -b feature/new-branch-name
  ```

* **Switch Branches**
  ```bash
  git checkout branch-name
  ```

* **Update Local Repository**
  ```bash
  git pull origin main
  ```

* **View Commit History**
  ```bash
  git log
  ```

## Best Practices
* Keep commits focused and meaningful
* Write descriptive commit messages
* Pull latest changes before pushing
* Use branches for new features/fixes
* Review changes before committing

## Troubleshooting
* If push is rejected, pull latest changes first:
  ```bash
  git pull --rebase origin main
  git push
  ```
* For authentication issues, use GitHub token or SSH key
* To undo last commit (before push):
  ```bash
  git reset --soft HEAD~1
  ```

