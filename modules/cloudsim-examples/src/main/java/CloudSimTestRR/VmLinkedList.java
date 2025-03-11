package CloudSimTestRR;

import org.cloudbus.cloudsim.Vm;

// Custom Linked List Implementation (Without Java List)
class VmNode {
    Vm vm;
    VmNode next;

    VmNode(Vm vm) {
        this.vm = vm;
        this.next = null;
    }
}

class VmLinkedList {
    private VmNode head, tail;
    
    // Insert VM at the end
    public void insert(Vm vm) {
        VmNode newNode = new VmNode(vm);
        if (head == null) {
            head = newNode;
            tail = newNode;
            tail.next = head; // Circular linked list
        } else {
            tail.next = newNode;
            tail = newNode;
            tail.next = head; // Maintain circular structure
        }
    }

    // Get and rotate VM (Round Robin logic)
    public Vm getNextVm() {
        if (head == null) return null;
        Vm vm = head.vm;
        head = head.next;
        tail = tail.next;
        return vm;
    }
}


