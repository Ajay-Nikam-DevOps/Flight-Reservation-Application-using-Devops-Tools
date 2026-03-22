pipeline{
    agent any 
    stages{
        stage('code-pull'){
            steps{
                 git branch: 'main', url: 'https://github.com/HritikBuwa/Flight-Reservation-Application-using-Devops-Tools.git' 
            }   
        }
        stage('Build'){
            steps{
                sh '''
                    cd frontend
                    npm install
                    npm run build
                '''
            }
        }
        stage('deploy'){
            steps{
                sh '''
                    cd frontend
                    aws s3 sync dist/ s3://cbz-frontend-project-bux-hritik/
                '''
            }
        }
    }
}