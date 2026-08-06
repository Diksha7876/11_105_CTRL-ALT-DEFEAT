pipeline {

    agent any

    environment {
        GIT_URL = 'https://github.com/Diksha7876/11_105_CTRL-ALT-DEFEAT.git'
        BRANCH = 'main'
    }

    stages {

        stage('Checkout Source') {
            steps {
                git branch: "${BRANCH}",
                    url: "${GIT_URL}"
            }
        }


        stage('Stop Existing Containers') {
            steps {
                dir('backend') {
                    sh 'docker-compose down || true'
                }
            }
        }

        

        stage('Build Docker Image') {
            steps {
                dir('backend') {
                    sh 'docker-compose build --no-cache'
                }
            }
        }

        stage('Deploy') {
            steps {
                dir('backend') {
                    sh 'docker-compose up -d'
                }
            }
        }

        stage('Verify') {
            steps {
                sh 'docker ps'
            }
        }
    }
}
