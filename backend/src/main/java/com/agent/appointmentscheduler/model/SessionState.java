package com.agent.appointmentscheduler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the conversation context so the Agent doesn't have amnesia.
 * This maintains state between turns in the conversation.
 */
public class SessionState {
    
    private Object currentUser;
    private List<Object> currentAppointments;
    
    public SessionState() {
        this.currentAppointments = new ArrayList<>();
    }
    
    public SessionState(Object currentUser, List<Object> currentAppointments) {
        this.currentUser = currentUser;
        this.currentAppointments = currentAppointments != null ? currentAppointments : new ArrayList<>();
    }
    
    public Object getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(Object currentUser) {
        this.currentUser = currentUser;
    }
    
    public List<Object> getCurrentAppointments() {
        return currentAppointments;
    }
    
    public void setCurrentAppointments(List<Object> currentAppointments) {
        this.currentAppointments = currentAppointments != null ? currentAppointments : new ArrayList<>();
    }
    
    /**
     * Clears all state
     */
    public void clear() {
        this.currentUser = null;
        this.currentAppointments.clear();
    }
    
    /**
     * Clears only appointments, keeping the user
     */
    public void clearAppointments() {
        this.currentAppointments.clear();
    }
}



