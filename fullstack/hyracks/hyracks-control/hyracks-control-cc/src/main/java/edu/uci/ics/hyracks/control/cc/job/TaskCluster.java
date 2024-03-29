/*
 * Copyright 2009-2010 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.hyracks.control.cc.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.uci.ics.hyracks.api.job.ActivityCluster;
import edu.uci.ics.hyracks.api.partitions.PartitionId;

public class TaskCluster {
    private final TaskClusterId taskClusterId;

    private final ActivityCluster ac;

    private final Task[] tasks;

    private final Set<PartitionId> producedPartitions;

    private final Set<PartitionId> requiredPartitions;

    private final Set<TaskCluster> dependencyTaskClusters;

    private final Set<TaskCluster> dependentTaskClusters;

    private final List<TaskClusterAttempt> taskClusterAttempts;

    public TaskCluster(TaskClusterId taskClusterId, ActivityCluster ac, Task[] tasks) {
        this.taskClusterId = taskClusterId;
        this.ac = ac;
        this.tasks = tasks;
        producedPartitions = new HashSet<PartitionId>();
        requiredPartitions = new HashSet<PartitionId>();
        dependencyTaskClusters = new HashSet<TaskCluster>();
        dependentTaskClusters = new HashSet<TaskCluster>();
        taskClusterAttempts = new ArrayList<TaskClusterAttempt>();
    }

    public TaskClusterId getTaskClusterId() {
        return taskClusterId;
    }

    public ActivityCluster getActivityCluster() {
        return ac;
    }

    public Task[] getTasks() {
        return tasks;
    }

    public Set<PartitionId> getProducedPartitions() {
        return producedPartitions;
    }

    public Set<PartitionId> getRequiredPartitions() {
        return requiredPartitions;
    }

    public Set<TaskCluster> getDependencyTaskClusters() {
        return dependencyTaskClusters;
    }

    public Set<TaskCluster> getDependentTaskClusters() {
        return dependentTaskClusters;
    }

    public List<TaskClusterAttempt> getAttempts() {
        return taskClusterAttempts;
    }

    @Override
    public String toString() {
        return "TC:" + Arrays.toString(tasks);
    }
}