*** Settings ***
Library           libraries/UtilLibrary.py
Variables         variables/Variables.py

*** Test Cases ***
Smoke
    [Setup]    InstallJaguar    ${JAGUAR_SYSTEM_TEST_IP}    ${USER}    ${PASSWORD}
    [Timeout]    60 minutes
    Execute Ssh Command     ${JAGUAR_SYSTEM_TEST_IP}    ${USER}    ${PASSWORD}    kubectl create -f /root/test.yaml && kubectl create -f /root/test1.yaml
    Sleep    10
    ${cmd_response}    Execute Ssh Command    ${JAGUAR_SYSTEM_TEST_IP}    ${USER}    ${PASSWORD}    kubectl get pods
    Should Contain    ${cmd_response}    Running
    Should Not Contain Any    ${cmd_response}    ContainerCreating
    Execute Ssh Command    ${JAGUAR_SYSTEM_TEST_IP}    ${USER}    ${PASSWORD}    kubectl delete -f /root/test.yaml && kubectl delete -f /root/test1.yaml
    [Teardown]    StopJaguar    ${JAGUAR_SYSTEM_TEST_IP}    ${USER}    ${PASSWORD}

*** Keywords ***
InstallJaguar
    [Arguments]    ${IP}    ${USER}    ${PASSWORD}
    Install And Startcontroller    ${IP}    ${USER}    ${PASSWORD}    /home/gitlab-runner/.m2/repository/com/sdnlab/jaguar-karaf/0.1.0-SNAPSHOT/jaguar-karaf-0.1.0-SNAPSHOT.zip    /home/jaguartest/
    Sleep    30

StopJaguar
    [Arguments]    ${IP}    ${USER}    ${PASSWORD}
    Stopcontroller    ${IP}    ${USER}    ${PASSWORD}    /home/jaguartest/jaguar
