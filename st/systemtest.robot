*** Settings ***
Library           libraries/UtilLibrary.py

*** Test Cases ***
Smoke
    [Setup]    InstallJaguar    10.42.118.80    zte    zte123
    [Timeout]    10 minutes
    Execute Ssh Command     10.42.118.80    zte    zte123    kubectl create -f /home/zte/pod-test.yaml && kubectl create -f /home/zte/pod2-test.yaml
    Sleep    10
    ${cmd_response}    Execute Ssh Command    10.42.118.80    zte    zte123    kubectl get pods
    Should Contain    ${cmd_response}    Running
    Should Not Contain Any    ${cmd_response}    ContainerCreating
    Execute Ssh Command    10.42.118.80    zte    zte123    kubectl delete -f /home/zte/pod-test.yaml && kubectl delete -f pod2-test.yaml
    [Teardown]    StopJaguar    10.42.118.80    zte    zte123

*** Keywords ***
InstallJaguar
    [Arguments]    ${IP}    ${USER}    ${PASSWORD}
    Install And Startcontroller    ${IP}    ${USER}    ${PASSWORD}    /home/zte/sdnlab/jaguar/karaf/target/jaguar-karaf-0.1.0-SNAPSHOT.zip    /home/zte/jaguartest/
    Wait For Controller Up    ${IP}

StopJaguar
    [Arguments]    ${IP}    ${USER}    ${PASSWORD}
    Stopcontroller    ${IP}    ${USER}    ${PASSWORD}    /home/zte/jaguartest/jaguar
