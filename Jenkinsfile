pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 10, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Project Files') {
            steps {
                script {
                    def requiredFiles = ['index.html', 'pom.xml', 'Dockerfile', '.github/workflows/maven.yml']
                    requiredFiles.each { f ->
                        if (!fileExists(f)) {
                            error("Required file missing: ${f}")
                        }
                    }
                }
            }
        }

        stage('Maven Build & Test') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B clean test'
                    } else {
                        bat 'mvn -B clean test'
                    }
                }
            }
        }

        stage('Maven Package') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B package -DskipTests'
                    } else {
                        bat 'mvn -B package -DskipTests'
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true, fingerprint: true
        }
        success {
            echo 'Pipeline completed successfully: Maven build, tests, and JAR packaging passed.'
        }
    }
}
