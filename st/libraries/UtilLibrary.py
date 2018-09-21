import requests
from SSHLibrary import SSHLibrary

import robot
import time
import re
import json
import warnings
import os

__author__ = "Basheeruddin Ahmed"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__email__ = "syedbahm@cisco.com"


global _cache

class RemoteHost:
    def __init__(self, host, user, password):
        self.host = host
        self.user = user
        self.password = password
        self.lib = SSHLibrary()
        self.lib.open_connection(self.host)
        self.lib.login(username=self.user, password=self.password)

    def __del__(self):
        self.lib.close_connection()

    def exec_cmd(self, command):
        print "Executing command " + command + " on host " + self.host
        rc = self.lib.execute_command(command, return_rc=True)
        if rc[1] != 0:
            raise Exception('remote command failed [{0}] with exit code {1}.'
                            'For linux-based vms, Please make sure requiretty is disabled in the /etc/sudoers file'
                            .format(command, rc))

    def mkdir(self, dir_name):
        self.exec_cmd("mkdir -p " + dir_name)

    def copy_file(self, src, dest):
        if src is None:
            print "src is None not copy anything to " + dest
            return

        if os.path.exists(src) is False:
            print "Src file " + src + " was not found"
            return

        print "Copying " + src + " to " + dest + " on " + self.host
        self.lib.put_file(src, dest)

    def kill_controller(self):
        self.exec_cmd("sudo ps axf | grep karaf | grep -v grep "
                      "| awk '{print \"kill -9 \" $1}' | sudo sh")

    def start_controller(self, dir_name):
        self.exec_cmd(dir_name + "/jaguar/bin/start")

def get(url, userId='admin', password='admin'):
    """Helps in making GET REST calls"""
    warnings.warn(
        "Use the Robot RequestsLibrary rather than this. See DatastoreCRUD.robot for examples",
        DeprecationWarning
    )
    headers = {}
    headers['Accept'] = 'application/xml'

    # Send the GET request
    session = _cache.switch("CLUSTERING_GET")
    resp = session.get(url, headers=headers, auth=(userId, password))
    # resp = session.get(url,headers=headers,auth={userId,password})
    # Read the response
    return resp


def nonprintpost(url, userId, password, data):
    """Helps in making POST REST calls without outputs"""
    warnings.warn(
        "Use the Robot RequestsLibrary rather than this. See DatastoreCRUD.robot for examples",
        DeprecationWarning
    )

    if userId is None:
        userId = 'admin'

    if password is None:
        password = 'admin'

    headers = {}
    headers['Content-Type'] = 'application/json'
    # headers['Accept']= 'application/xml'

    session = _cache.switch("CLUSTERING_POST")
    resp = session.post(url, data.encode('utf-8'), headers=headers, auth=(userId, password))

    return resp


def post(url, userId, password, data):
    """Helps in making POST REST calls"""
    warnings.warn(
        "Use the Robot RequestsLibrary rather than this. See DatastoreCRUD.robot for examples",
        DeprecationWarning
    )

    if userId is None:
        userId = 'admin'

    if password is None:
        password = 'admin'

    print("post request with url " + url)
    print("post request with data " + data)
    headers = {}
    headers['Content-Type'] = 'application/json'
    # headers['Accept'] = 'application/xml'
    session = _cache.switch("CLUSTERING_POST")
    resp = session.post(url, data.encode('utf-8'), headers=headers, auth=(userId, password))

    # print(resp.raise_for_status())
    print(resp.headers)
    if resp.status_code >= 500:
        print(resp.text)

    return resp


def delete(url, userId='admin', password='admin'):
    """Helps in making DELET REST calls"""
    warnings.warn(
        "Use the Robot RequestsLibrary rather than this. See DatastoreCRUD.robot for examples",
        DeprecationWarning
    )
    print("delete all resources belonging to url" + url)
    session = _cache.switch("CLUSTERING_DELETE")
    resp = session.delete(url, auth=(userId, password))  # noqa


def Should_Not_Be_Type_None(var):
    '''Keyword to check if the given variable is of type NoneType.  If the
        variable type does match  raise an assertion so the keyword will fail
    '''
    if var is None:
        raise AssertionError('the variable passed was type NoneType')
    return 'PASS'


def execute_ssh_command(ip, username, password, command):
    """Execute SSH Command

    use username and password of controller server for ssh and need
    karaf distribution location like /root/Documents/dist
    """
    print "executing ssh command"
    lib = SSHLibrary()
    lib.open_connection(ip)
    lib.login(username=username, password=password)
    print "login done"
    cmd_response = lib.execute_command(command)
    print "command executed : " + command
    lib.close_connection()
    return cmd_response


def wait_for_controller_up(ip, port="8181"):
    url = "http://" + ip + ":" + str(port) + \
          "/restconf/modules"

    print "Waiting for controller " + ip + " up."
    # Try 30*10s=5 minutes for the controller to be up.
    for i in xrange(30):
        try:
            print "attempt " + str(i) + " to url " + url
            resp = get(url, "admin", "admin")
            print "attempt " + str(i) + " response is " + str(resp)
            print resp.status_code
            if resp.status_code == 200:
                print "Wait for controller " + ip + " succeeded"
                return True
        except Exception as e:
            print e
        time.sleep(10)

    print "Wait for controller " + ip + " failed"
    return False


def startAllControllers(username, password, karafhome, port, *ips):
    # Start all controllers
    for ip in ips:
        execute_ssh_command(ip, username, password, karafhome + "/bin/start")

    # Wait for all of them to be up
    for ip in ips:
        rc = wait_for_controller_up(ip, port)
        if rc is False:
            return False
    return True


def startcontroller(ip, username, password, karafhome, port):
    execute_ssh_command(ip, username, password, karafhome + "/bin/start")
    return wait_for_controller_up(ip, port)


def stopcontroller(ip, username, password, karafhome):
    executeStopController(ip, username, password, karafhome)

    wait_for_controller_stopped(ip, username, password, karafhome)


def executeStopController(ip, username, password, karafhome):
    execute_ssh_command(ip, username, password, karafhome + "/bin/stop")


def stopAllControllers(username, password, karafhome, *ips):
    for ip in ips:
        executeStopController(ip, username, password, karafhome)

    for ip in ips:
        wait_for_controller_stopped(ip, username, password, karafhome)


def wait_for_controller_stopped(ip, username, password, karafHome):
    lib = SSHLibrary()
    lib.open_connection(ip)
    lib.login(username=username, password=password)

    # Wait 1 minute for the controller to stop gracefully
    tries = 20
    i = 1
    while i <= tries:
        stdout = lib.execute_command("ps -axf | grep karaf | grep -v grep | wc -l")
        # print "stdout: "+stdout
        processCnt = stdout[0].strip('\n')
        print("processCnt: " + processCnt)
        if processCnt == '0':
            break
        i = i + 1
        time.sleep(3)

    lib.close_connection()

    if i > tries:
        print "Killing controller"
        kill_controller(ip, username, password, karafHome)


def clean_journal(ip, username, password, karafHome):
    execute_ssh_command(ip, username, password, "rm -rf " + karafHome + "/journal")


def kill_controller(ip, username, password, karafHome):
    execute_ssh_command(ip, username, password,
                        "ps axf | grep karaf | grep -v grep | awk '{print \"kill -9 \" $1}' | sh")


def isolate_controller(controllers, username, password, isolated):
    """ Isolate one controller from the others in the cluster

    :param controllers: A list of ip addresses or host names as strings.
    :param username: Username for the controller to be isolated.
    :param password: Password for the controller to be isolated.
    :param isolated: Number (starting at one) of the controller to be isolated.
    :return: If successful, returns "pass", otherwise returns the last failed IPTables text.
    """
    isolated_controller = controllers[isolated - 1]
    for controller in controllers:
        if controller != isolated_controller:
            base_str = 'sudo iptables -I OUTPUT -p all --source '
            cmd_str = base_str + isolated_controller + ' --destination ' + controller + ' -j DROP'
            execute_ssh_command(isolated_controller, username, password, cmd_str)
            cmd_str = base_str + controller + ' --destination ' + isolated_controller + ' -j DROP'
            execute_ssh_command(isolated_controller, username, password, cmd_str)
    ip_tables = execute_ssh_command(isolated_controller, username, password, 'sudo iptables -L')
    print ip_tables
    iso_result = 'pass'
    for controller in controllers:
        controller_regex_string = "[\s\S]*" + isolated_controller + " *" + controller + "[\s\S]*"
        controller_regex = re.compile(controller_regex_string)
        if not controller_regex.match(ip_tables):
            iso_result = ip_tables
        controller_regex_string = "[\s\S]*" + controller + " *" + isolated_controller + "[\s\S]*"
        controller_regex = re.compile(controller_regex_string)
        if not controller_regex.match(ip_tables):
            iso_result = ip_tables
    return iso_result


def rejoin_controller(controllers, username, password, isolated):
    """ Return an isolated controller to the cluster.

    :param controllers: A list of ip addresses or host names as strings.
    :param username: Username for the isolated controller.
    :param password: Password for the isolated controller.
    :param isolated: Number (starting at one) of the isolated controller isolated.
    :return: If successful, returns "pass", otherwise returns the last failed IPTables text.
    """
    isolated_controller = controllers[isolated - 1]
    for controller in controllers:
        if controller != isolated_controller:
            base_str = 'sudo iptables -D OUTPUT -p all --source '
            cmd_str = base_str + isolated_controller + ' --destination ' + controller + ' -j DROP'
            execute_ssh_command(isolated_controller, username, password, cmd_str)
            cmd_str = base_str + controller + ' --destination ' + isolated_controller + ' -j DROP'
            execute_ssh_command(isolated_controller, username, password, cmd_str)
    ip_tables = execute_ssh_command(isolated_controller, username, password, 'sudo iptables -L')
    print ip_tables
    iso_result = 'pass'
    for controller in controllers:
        controller_regex_string = "[\s\S]*" + isolated_controller + " *" + controller + "[\s\S]*"
        controller_regex = re.compile(controller_regex_string)
        if controller_regex.match(ip_tables):
            iso_result = ip_tables
        controller_regex_string = "[\s\S]*" + controller + " *" + isolated_controller + "[\s\S]*"
        controller_regex = re.compile(controller_regex_string)
        if controller_regex.match(ip_tables):
            iso_result = ip_tables
    return iso_result


def flush_iptables(controllers, username, password):
    """Removes all entries from IPTables on all controllers.

    :param controllers: A list of ip address or host names as strings.
    :param username: Username for all controllers.
    :param password: Password for all controllers.
    :return: If successful, returns "pass", otherwise returns "fail".
    """
    flush_result = 'pass'
    for controller in controllers:
        print 'Flushing ' + controller
        cmd_str = 'sudo iptables -v -F'
        cmd_result = execute_ssh_command(controller, username, password, cmd_str)
        print cmd_result
        success_string = "Flushing chain `INPUT'" + "\n"
        success_string += "Flushing chain `FORWARD'" + "\n"
        success_string += "Flushing chain `OUTPUT'"
        if not cmd_result == success_string:
            flush_result = "Failed to flush IPTables. Check Log."
        print "."
        print "."
        print "."
    return flush_result


def build_elastic_search_JSON_request(query_String):
    data = {'from': '0',
            'size': '1',
            'sort': [{'TimeStamp': {'order': 'desc'}}],
            'query': {'query_string': {'query': query_String}}}
    return json.dumps(data)


def create_query_string_search(data_category, metric_name, node_id, rk_node_id):
    query = 'TSDRDataCategory:'
    query += data_category
    query += ' AND MetricName:'
    query += metric_name
    query += ' AND NodeID:\"'
    query += node_id
    query += '\" AND RecordKeys.KeyValue:\"'
    query += rk_node_id
    query += '\" AND RecordKeys.KeyName:Node AND RecordKeys.KeyValue:0 AND RecordKeys.KeyName:Table'
    return query


def create_query_string_count(data_category):
    query = 'TSDRDataCategory:'
    query += data_category
    return query


def extract_metric_value_search(response):
    return str(response['hits']['hits'][0]['_source']['MetricValue'])


def extract_metric_value_count(response):
    return int(response['hits']['total'])

def install_and_startcontroller(host, user, password, distribution, dir_name):
    distribution_name \
        = os.path.splitext(os.path.basename(distribution))[0]
    distribution_ver = re.search('(\d+\.\d+\.\d+-\w+\Z)|'
                                 '(\d+\.\d+\.\d+-\w+)(-RC\d+\Z)|'
                                 '(\d+\.\d+\.\d+-\w+)(-RC\d+(\.\d+)\Z)|'
                                 '(\d+\.\d+\.\d+-\w+)(-SR\d+\Z)|'
                                 '(\d+\.\d+\.\d+-\w+)(-SR\d+(\.\d+)\Z)',
                                 distribution_name)  # noqa

    if distribution_ver is None:
        print distribution_name + " is not a valid distribution version." \
                                  " (Must contain version in the form: " \
                                  "\"<#>.<#>.<#>-<name>\" or \"<#>.<#>." \
                                  "<#>-<name>-SR<#>\" or \"<#>.<#>.<#>" \
                                  "-<name>-RC<#>\", e.g. 0.2.0-SNAPSHOT)"
        sys.exit(1)
    distribution_ver = distribution_ver.group()

    remote = RemoteHost(host, user, password)

    remote.exec_cmd("rm -rf " + dir_name)
    # Create the deployment directory
    remote.mkdir(dir_name)

    # Copy the distribution to the host and unzip it
    odl_file_path = dir_name + "/jaguar.zip"
    remote.copy_file(distribution, odl_file_path)
    remote.exec_cmd("unzip -o " + odl_file_path + " -d " +
                         dir_name + "/")

    # Rename the distribution directory to jaguar
    remote.exec_cmd("mv " + dir_name + "/" +
                         distribution_name + " " + dir_name + "/jaguar")


    # Run karaf
    remote.start_controller(dir_name)
#
# main invoked
if __name__ != "__main__":
    _cache = robot.utils.ConnectionCache('No sessions created')
    # here create one session for each HTTP functions
    _cache.register(requests.session(), alias='CLUSTERING_GET')
    _cache.register(requests.session(), alias='CLUSTERING_POST')
    _cache.register(requests.session(), alias='CLUSTERING_DELETE')