import subprocess


def call_popen(cmd):
    child = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    output = child.communicate()
    if child.returncode:
        raise RuntimeError("Fatal error executing %s" % (cmd))
    if len(output) == 0 or output[0] is None:
        output = ""
    else:
        output = output[0].decode("utf8").strip()
    return output


def call_prog(prog, args_list):
    cmd = [prog, "--timeout=5", "-vconsole:off"] + args_list
    return call_popen(cmd)


def ovs_vsctl(*args):
    return call_prog("ovs-vsctl", list(args))


def ovs_ofctl(*args):
    return call_prog("ovs-ofctl", list(args))

if __name__ == '__main__':
    print ovs_vsctl('show')

