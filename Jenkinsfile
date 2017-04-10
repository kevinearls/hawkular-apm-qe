node {
    stage('Setup environment') {
        // All of the values listed here are required parameters for this job
        echo 'Run with: '
        echo 'HAWKULAR_APM_URI ' + HAWKULAR_APM_URI
        echo 'HAWKULAR_APM_USERNAME ' + HAWKULAR_APM_USERNAME
        echo 'HAWKULAR_APM_PASSWORD ' + HAWKULAR_APM_PASSWORD
        echo 'HAWKULAR_SERVICE_NAME ' + HAWKULAR_SERVICE_NAME

        // Set up environment for tests
        def M2_HOME = tool 'maven-3.3.9'
        def JAVA_HOME = tool 'jdk8'
        env.M2_HOME = "${M2_HOME}"
        env.JAVA_HOME = "${JAVA_HOME}"

        env.PATH = "${M2_HOME}/bin:${JAVA_HOME}/bin:${env.PATH}"
    }
    stage('Kill Existing Instance') {
        sh "docker ps | grep hawkular-apm  | awk '{print \$1}' | xargs docker stop || true"
    }
    stage('Startup APM Server') {
        sh "BUILD_ID=dontKillMe nohup docker run -p 8080:8080 jboss/hawkular-apm-server-dev &"
    }
    stage('Wait until server is available') {
        timeout(time: 5, unit: 'MINUTES') {
            waitUntil {
                def r = sh script: 'wget -q ' + HAWKULAR_APM_URI + '/hawkular-ui/apm -O /dev/null', returnStatus: true
                return (r == 0);
            }
        }

        sleep 10 // FIXME I had to add this for Docker, but shouldn't need it
    }
    stage('Build and run tests') {
        //git 'https://github.com/Hawkular-QE/hawkular-apm-qe.git'
        sh "mvn -Dmaven.test.failure.ignore clean test"
    }
    stage('Record test results') {
        junit '**/target/surefire-reports/TEST-*.xml'
    }
    stage('populate') {
        build job: 'populate-apm-pipeline', parameters:
            [[$class: 'StringParameterValue', name: 'HAWKULAR_APM_URI', value: HAWKULAR_APM_URI],    // FIXME! Use shorter syntax
            [$class: 'StringParameterValue', name: 'HAWKULAR_APM_USERNAME', value: HAWKULAR_APM_USERNAME],
            [$class: 'StringParameterValue', name: 'HAWKULAR_SERVICE_NAME', value: HAWKULAR_SERVICE_NAME],
            [$class: 'StringParameterValue', name: 'HAWKULAR_APM_PASSWORD', value: HAWKULAR_APM_PASSWORD]]
    }
    
}
