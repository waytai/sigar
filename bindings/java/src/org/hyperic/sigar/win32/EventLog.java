/*
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of SIGAR.
 * 
 * SIGAR is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.sigar.win32;

public class EventLog extends Win32 {

    int eventLogHandle = 0;  // holds the event log HANDLE

    public static final String SYSTEM      = "System";
    public static final String APPLICATION = "Application";
    public static final String SECURITY    = "Security";

    // Event log types as defined in WINNT.H
    public static final int EVENTLOG_SUCCESS          = 0x0000;
    public static final int EVENTLOG_ERROR_TYPE       = 0x0001;
    public static final int EVENTLOG_WARNING_TYPE     = 0x0002;
    public static final int EVENTLOG_INFORMATION_TYPE = 0x0004;
    public static final int EVENTLOG_AUDIT_SUCCESS    = 0x0008;
    public static final int EVENTLOG_AUDIT_FAILURE    = 0x0010;

    // Event log timeouts
    public static final int EVENTLOG_WAIT_INFINITE    = -1;
    private String name;

    /**
     * Create an event log.
     */
    public EventLog() {}

    /**
     * Open the event log.  This must be done before any other operation.
     * @param name Name of the event log to open.
     * For example: "Application", "System" or "Security".
     * @exception Win32Exception If opening the event log fails.
     */
    public void open(String name) throws Win32Exception {
        this.name = name;
        openlog(name);
    }

    public native void openlog(String name) throws Win32Exception;

    /**
     * Close the event log.
     * @exception Win32Excepion If closing of the event log fails.
     */
    public native void close() throws Win32Exception;

    /**
     * Get the number of records for this event log
     * @exception Win32Exception If the event log is not open
     */
    public native int getNumberOfRecords() throws Win32Exception;

    /**
     * Get the oldest event log record
     * @exception Win32Exception If the event log is not open
     */
    public native int getOldestRecord() throws Win32Exception;

    /**
     * Get the newest event log record.
     * @exception Win32Exception If the event log is not open
     */
    public int getNewestRecord() 
        throws Win32Exception
    {
        return getOldestRecord() + getNumberOfRecords() - 1;
    }

    /**
     * Read an event log record.  This method only support the
     * EVENTLOG_SEEK_READ flag, no sequential reading is currently
     * supported.
     * 
     * @param recordOffset The record offset to read.
     * @exception Win32Exception If the event log is not open, or
     *                           if the specified record could not be
     *                           found
     */
    public EventLogRecord read(int recordOffset)
        throws Win32Exception {

        EventLogRecord record = readlog(this.name, recordOffset);
        record.setLogName(this.name);
        return record;
    }

    private native EventLogRecord readlog(String name, int recordOffset)
        throws Win32Exception;

    /**
     * Wait for a change to the event log.  This function will
     * return on event log change, or when the timeout pops.
     *
     * Windows PulseEvent will fire no more than once per 5 seconds,
     * so multiple events may occur before the application is notified.
     *
     * @param timeout Time to wait in milliseconds.
     * @exception Win32Exception If the event log is not open, or
     *                           there is an error waiting for the
     *                           the event.
     */
    public native void waitForChange(int timeout)
        throws Win32Exception;

    /**
     * Eventlog names are store in the registry under:
     * SYSTEM\CurrentControlSet\Services\Eventlog
     * This method returns the list of these entries.
     * @return All Eventlog names
     */
    public static String[] getLogNames() {
        final String EVENTLOG_KEY =
            "SYSTEM\\CurrentControlSet\\Services\\Eventlog";

        String[] names;
        RegistryKey key = null;
        try {
            key = RegistryKey.LocalMachine.openSubKey(EVENTLOG_KEY);
            names = key.getSubKeyNames();
        } catch (Win32Exception e) {
            names =
                new String[] { SYSTEM, APPLICATION, SECURITY };
        } finally {
            if (key != null) {
                key.close();
            }
        }

        return names;
    }
}
