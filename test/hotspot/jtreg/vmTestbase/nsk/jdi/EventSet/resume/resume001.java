/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.jdi.EventSet.resume;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

import static nsk.share.Consts.TEST_FAILED;

/**
 * The test for the implementation of an object of the type
 * EventSet.
 *
 * The test checks that results of the method
 * <code>com.sun.jdi.EventSet.resume()</code>
 * complies with its spec.
 *
 * Test cases include all three possible suspensions, NONE,
 * EVENT_THREAD, and ALL, for ClassPrepareEvent sets.
 *
 * To check up on the method, a debugger,
 * upon getting new set for ClassPrepareEvent,
 * suspends VM with the method VirtualMachine.suspend(),
 * gets the List of geduggee's threads calling VM.allThreads(),
 * invokes the method EventSet.resume(), and
 * gets another List of geduggee's threads.
 * The debugger then compares values of
 * each thread's suspendCount from first and second Lists.
 *
 * The test works as follows.
 * - The debugger resumes the debuggee and
 *   waits for the ClassPrepareEvents.
 * - The debuggee creates three threads, running one by one and
 *   making special clases loaded and prepared to create the Events
 * - Upon getting the Events,
 *   the debugger performs the check required.
 */

public class resume001 extends TestDebuggerType1 {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        debuggeeName = "nsk.jdi.EventSet.resume.resume001a";
        return new resume001().runThis(argv, out);
    }

    protected void testRun() {

        final int SUSPEND_NONE   = EventRequest.SUSPEND_NONE;
        final int SUSPEND_THREAD = EventRequest.SUSPEND_EVENT_THREAD;
        final int SUSPEND_ALL    = EventRequest.SUSPEND_ALL;

        String classNames[] = {
            "nsk.jdi.EventSet.resume.TestClass2",
            "nsk.jdi.EventSet.resume.TestClass3",
            "nsk.jdi.EventSet.resume.TestClass4",
        };

        EventRequest eventRequest;

        for (int i = 0; ; i++) {

            if (!shouldRunAfterBreakpoint()) {
                vm.resume();
                break;
            }


            display(":::::: case: # " + i);

            switch (i) {

                case 0:
                eventRequest = settingClassPrepareRequest(classNames[0],
                                   SUSPEND_NONE,   "ClassPrepareRequest0");
                break;

                case 1:
                eventRequest = settingClassPrepareRequest(classNames[1],
                                   SUSPEND_THREAD, "ClassPrepareRequest1");
                break;

                case 2:
                eventRequest = settingClassPrepareRequest(classNames[2],
                                   SUSPEND_ALL,    "ClassPrepareRequest2");
                break;


                default:
                throw new Failure("** default case 2 **");
            }

            display("......waiting for new ClassPrepareEvent : " + i);
            EventSet eventSet = eventHandler.waitForRequestedEventSet(new EventRequest[]{eventRequest}, waitTime, true);

            EventIterator eventIterator = eventSet.eventIterator();
            Event newEvent = eventIterator.nextEvent();

            if ( !(newEvent instanceof ClassPrepareEvent)) {
                setFailedStatus("ERROR: new event is not ClassPreparedEvent");
            } else {

                String property = (String) newEvent.request().getProperty("number");
                display("       got new ClassPrepareEvent with propety 'number' == " + property);

                display("......checking up on EventSet.resume()");
                display("......--> vm.suspend();");
                vm.suspend();

                display("        getting : Map<String, Integer> suspendsCounts1");

                Map<String, Integer> suspendsCounts1 = new HashMap<String, Integer>();
                for (ThreadReference threadReference : vm.allThreads()) {
                    suspendsCounts1.put(threadReference.name(), threadReference.suspendCount());
                }
                display(suspendsCounts1.toString());

                display("        eventSet.resume;");
                eventSet.resume();

                display("        getting : Map<String, Integer> suspendsCounts2");
                Map<String, Integer> suspendsCounts2 = new HashMap<String, Integer>();
                for (ThreadReference threadReference : vm.allThreads()) {
                    suspendsCounts2.put(threadReference.name(), threadReference.suspendCount());
                }
                display(suspendsCounts2.toString());

                display("        getting : int policy = eventSet.suspendPolicy();");
                int policy = eventSet.suspendPolicy();

                switch (policy) {

                  case SUSPEND_NONE   :
                       display("        case SUSPEND_NONE");
                       for (String threadName : suspendsCounts1.keySet()) {
                           display("        checking " + threadName);
                           if (!suspendsCounts2.containsKey(threadName)) {
                               complain("ERROR: couldn't get ThreadReference for " + threadName);
                               testExitCode = TEST_FAILED;
                               break;
                           }
                           int count1 = suspendsCounts1.get(threadName);
                           int count2 = suspendsCounts2.get(threadName);
                           if (count1 != count2) {
                               complain("ERROR: suspendCounts don't match for : " + threadName);
                               complain("before resuming : " + count1);
                               complain("after  resuming : " + count2);
                               testExitCode = TEST_FAILED;
                               break;
                           }
                       }
                       break;

                  case SUSPEND_THREAD :
                       display("        case SUSPEND_THREAD");
                       for (String threadName : suspendsCounts1.keySet()) {
                           display("checking " + threadName);
                           if (!suspendsCounts2.containsKey(threadName)) {
                               complain("ERROR: couldn't get ThreadReference for " + threadName);
                               testExitCode = TEST_FAILED;
                               break;
                           }
                           int count1 = suspendsCounts1.get(threadName);
                           int count2 = suspendsCounts2.get(threadName);
                           String eventThreadName = ((ClassPrepareEvent)newEvent).thread().name();
                           int expectedValue = count2 + (eventThreadName.equals(threadName) ? 1 : 0);
                           if (count1 != expectedValue) {
                               complain("ERROR: suspendCounts don't match for : " + threadName);
                               complain("before resuming : " + count1);
                               complain("after  resuming : " + count2);
                               testExitCode = TEST_FAILED;
                               break;
                           }
                       }
                       break;

                    case SUSPEND_ALL    :

                        display("        case SUSPEND_ALL");
                        for (String threadName : suspendsCounts1.keySet()) {
                            display("checking " + threadName);

                            if (!newEvent.request().equals(eventRequest))
                                break;
                            if (!suspendsCounts2.containsKey(threadName)) {
                                complain("ERROR: couldn't get ThreadReference for " + threadName);
                                testExitCode = TEST_FAILED;
                                break;
                            }
                            int count1 = suspendsCounts1.get(threadName);
                            int count2 = suspendsCounts2.get(threadName);
                            if (count1 != count2 + 1) {
                                complain("ERROR: suspendCounts don't match for : " + threadName);
                                complain("before resuming : " + count1);
                                complain("after  resuming : " + count2);
                                testExitCode = TEST_FAILED;
                                break;
                            }
                        }
                        break;

                     default: throw new Failure("** default case 1 **");
                }
            }

            display("......--> vm.resume()");
            vm.resume();
        }
        return;
    }

    private ClassPrepareRequest settingClassPrepareRequest ( String  testedClass,
                                                             int     suspendPolicy,
                                                             String  property       )
            throws Failure {
        try {
            display("......setting up ClassPrepareRequest:");
            display("       class: " + testedClass + "; property: " + property);

            ClassPrepareRequest
            cpr = eventRManager.createClassPrepareRequest();
            cpr.putProperty("number", property);
            cpr.addClassFilter(testedClass);
            cpr.setSuspendPolicy(suspendPolicy);

            display("      ClassPrepareRequest has been set up");
            return cpr;
        } catch ( Exception e ) {
            throw new Failure("** FAILURE to set up ClassPrepareRequest **");
        }
    }

}