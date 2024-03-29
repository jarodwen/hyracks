package edu.uci.ics.hivesterix.perf.base;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestSuite;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;

import edu.uci.ics.hivesterix.common.config.ConfUtil;
import edu.uci.ics.hyracks.control.cc.ClusterControllerService;
import edu.uci.ics.hyracks.control.common.controllers.CCConfig;
import edu.uci.ics.hyracks.control.common.controllers.NCConfig;
import edu.uci.ics.hyracks.control.nc.NodeControllerService;

@SuppressWarnings("deprecation")
public abstract class AbstractPerfTestSuiteClass extends TestSuite {

    private static final String PATH_TO_HADOOP_CONF = "src/test/resources/perf/hadoop/conf";
    private static final String PATH_TO_HIVE_CONF = "src/test/resources/perf/hive/conf/hive-default.xml";
    private static final String PATH_TO_DATA = "src/test/resources/perf/data/";

    private MiniDFSCluster dfsCluster;
    private MiniMRCluster mrCluster;

    private JobConf conf = new JobConf();
    protected FileSystem dfs;

    private int numberOfNC = 2;
    private ClusterControllerService cc;
    private Map<String, NodeControllerService> ncs = new HashMap<String, NodeControllerService>();

    /**
     * setup cluster
     * 
     * @throws IOException
     */
    protected void setup() throws Exception {
        setupHdfs();
        setupHyracks();
    }

    private void setupHdfs() throws IOException {
        conf.addResource(new Path(PATH_TO_HADOOP_CONF + "/core-site.xml"));
        conf.addResource(new Path(PATH_TO_HADOOP_CONF + "/mapred-site.xml"));
        conf.addResource(new Path(PATH_TO_HADOOP_CONF + "/hdfs-site.xml"));
        HiveConf hconf = new HiveConf(SessionState.class);
        hconf.addResource(new Path(PATH_TO_HIVE_CONF));

        FileSystem lfs = FileSystem.getLocal(new Configuration());
        lfs.delete(new Path("build"), true);
        lfs.delete(new Path("metastore_db"), true);

        System.setProperty("hadoop.log.dir", "logs");
        dfsCluster = new MiniDFSCluster(hconf, numberOfNC, true, null);
        dfs = dfsCluster.getFileSystem();

        mrCluster = new MiniMRCluster(2, dfs.getUri().toString(), 1);
        hconf.setVar(HiveConf.ConfVars.HADOOPJT, "localhost:" + mrCluster.getJobTrackerPort());
        hconf.setInt("mapred.min.split.size", 1342177280);

        conf = new JobConf(hconf);
        ConfUtil.setJobConf(conf);

        String fsName = conf.get("fs.default.name");
        hconf.set("hive.metastore.warehouse.dir", fsName.concat("/tmp/hivesterix"));
        String warehouse = hconf.get("hive.metastore.warehouse.dir");
        dfs.mkdirs(new Path(warehouse));
        ConfUtil.setHiveConf(hconf);
    }

    private void setupHyracks() throws Exception {
        // read hive conf
        HiveConf hconf = new HiveConf(SessionState.class);
        hconf.addResource(new Path(PATH_TO_HIVE_CONF));
        SessionState.start(hconf);
        String ipAddress = hconf.get("hive.hyracks.host");
        int clientPort = Integer.parseInt(hconf.get("hive.hyracks.port"));
        int clusterPort = clientPort;

        // start hyracks cc
        CCConfig ccConfig = new CCConfig();
        ccConfig.clientNetIpAddress = ipAddress;
        ccConfig.clientNetPort = clientPort;
        ccConfig.clusterNetPort = clusterPort;
        ccConfig.profileDumpPeriod = 1000;
        ccConfig.heartbeatPeriod = 200000000;
        ccConfig.maxHeartbeatLapsePeriods = 200000000;
        cc = new ClusterControllerService(ccConfig);
        cc.start();

        // start hyracks nc
        for (int i = 0; i < numberOfNC; i++) {
            NCConfig ncConfig = new NCConfig();
            ncConfig.ccHost = ipAddress;
            ncConfig.clusterNetIPAddress = ipAddress;
            ncConfig.ccPort = clientPort;
            ncConfig.dataIPAddress = "127.0.0.1";
            ncConfig.datasetIPAddress = "127.0.0.1";
            ncConfig.nodeId = "nc" + i;
            NodeControllerService nc = new NodeControllerService(ncConfig);
            nc.start();
            ncs.put(ncConfig.nodeId, nc);
        }
    }

    protected void makeDir(String path) throws IOException {
        dfs.mkdirs(new Path(path));
    }

    protected void loadFiles(String src, String dest) throws IOException {
        dfs.copyFromLocalFile(new Path(src), new Path(dest));
    }

    protected void cleanup() throws Exception {
        cleanupHdfs();
        cleanupHyracks();
    }

    /**
     * cleanup hdfs cluster
     */
    private void cleanupHdfs() throws IOException {
        dfs.delete(new Path("/"), true);
        FileSystem.closeAll();
        dfsCluster.shutdown();
    }

    /**
     * cleanup hyracks cluster
     */
    private void cleanupHyracks() throws Exception {
        Iterator<NodeControllerService> iterator = ncs.values().iterator();
        while (iterator.hasNext()) {
            NodeControllerService nc = iterator.next();
            nc.stop();
        }
        cc.stop();
    }

    protected static List<String> getIgnoreList(String ignorePath) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(ignorePath));
        String s = null;
        List<String> ignores = new ArrayList<String>();
        while ((s = reader.readLine()) != null) {
            ignores.add(s);
        }
        reader.close();
        return ignores;
    }

    protected static boolean isIgnored(String q, List<String> ignoreList) {
        for (String ignore : ignoreList) {
            if (ignore.equals(q)) {
                return true;
            }
        }
        return false;
    }

    protected void loadData() throws IOException {

        makeDir("/tpch");
        makeDir("/tpch/customer");
        makeDir("/tpch/lineitem");
        makeDir("/tpch/orders");
        makeDir("/tpch/part");
        makeDir("/tpch/partsupp");
        makeDir("/tpch/supplier");
        makeDir("/tpch/nation");
        makeDir("/tpch/region");

        makeDir("/jarod");

        loadFiles(PATH_TO_DATA + "customer.tbl", "/tpch/customer/");
        loadFiles(PATH_TO_DATA + "lineitem.tbl", "/tpch/lineitem/");
        loadFiles(PATH_TO_DATA + "orders.tbl", "/tpch/orders/");
        loadFiles(PATH_TO_DATA + "part.tbl", "/tpch/part/");
        loadFiles(PATH_TO_DATA + "partsupp.tbl", "/tpch/partsupp/");
        loadFiles(PATH_TO_DATA + "supplier.tbl", "/tpch/supplier/");
        loadFiles(PATH_TO_DATA + "nation.tbl", "/tpch/nation/");
        loadFiles(PATH_TO_DATA + "region.tbl", "/tpch/region/");

        loadFiles(PATH_TO_DATA + "ext-gby.tbl", "/jarod/");
    }

}
