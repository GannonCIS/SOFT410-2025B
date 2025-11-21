pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Start Building..'
                sh './gradlew build'

            }
        }
        stage('Test') {
            steps {
                echo 'Starting Testing..'
                sh './gradlew test'
            }
            post {
               always {
                   echo 'Publishing Test Results...'

               junit "build/reports/tests/test/*.xml"

                publishHTML (target: [
                   allowMissing: false,
                   alwaysLinkToLastBuild: false,
                   keepAll: true,
                   reportDir: 'build/reports/tests/test',
                   reportFiles: 'index.html',
                   reportName: 'Test Report'
               ])
               }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }
    }
}