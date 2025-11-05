pipeline {
    agent any // Specifies that the pipeline can run on any available agent

    stages {
        stage('Build') { // Defines a stage named 'Build'
            steps {
                echo 'Building the project...' // Executes a simple shell command
                // Add your build commands here, e.g., sh 'mvn clean install'
            }
        }
        stage('Test') { // Defines a stage named 'Test'
            steps {
                echo 'Running tests...' // Executes a simple shell command
                // Add your test commands here, e.g., sh 'mvn test'
                // junit '**/target/*.xml' // Example for publishing JUnit test results
            }
        }
        stage('Deploy') { // Defines a stage named 'Deploy'
            steps {
                echo 'Deploying application...' // Executes a simple shell command
                // Add your deployment commands here, e.g., sh 'scp target/app.jar user@server:/path'
            }
        }
    }
}