pipeline {

    agent any

    environment {
        GIT_URL = 'https://github.com/Neueda-Learning/11_105_CTRL-ALT-DEFEAT.git'
        BRANCH = 'main'
    }

    stages {

        stage('Checkout Source') {
            steps {
                git branch: "${BRANCH}",
                    url: "${GIT_URL}"
            }
        }

        stage('Build Spring Boot') {
            steps {
                dir('backend') {
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Stop Existing Containers') {
            steps {
                dir('backend') {
                    sh 'docker-compose down || true'
                }
            }
        }

        stage('Debug Files') {
            steps {
                sh '''
                pwd
                ls -la
                find . -name Dockerfile -o -name dockerfile
                '''
                dir('backend') {
                    sh '''
                    pwd
                    ls -la
                    '''
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
