class ConnectorSetUp:
    """
    The class responsible for setting up the tables in the Cassandra database
    that will be used in the map & reduce steps later on.

    The keyspaces created will be named by the form 'PREFIX_N', where PREFIX
    is specified in the constructor and N is an integer incrementing from 0.

    The basic useage of the class is:
    1. Initialize an instance of the class.
    2. Connect to the Cassandra database.
    3. Generate the keyspaces & tables.
    4. Populate the tables.
    """

    def __init__(self, cluster, nKeyspaces, identifyKspc, keyspacePrefix = 'connector'):
        """
        Store the cluster object, but don't connect to it yet.

        Args:
            cluster (DATASTAX cluster): The cluster object to target.
                By allowing the user to initialize the cluster object, they
                retain full control over the IP, port, load policy, etc.
            nKeyspaces (integer): The number of keyspaces in the domain.
            identifyKspc ( (uid) -> int ): A function that takes a uid and returns the
                corresponding keyspace.
            keyspacePrefix (string, optional): Prefix used for naming the keyspaces.
                Defaults to 'Connector'.
        """
        self.cluster = cluster
        self.keyspaceNames = [keyspacePrefix + '_' + `i` for i in range(nKeyspaces)]
        self.identifyKspc = identifyKspc

    def connect(self):
        """
        Call connect on the cluster object.
        """
        self.session = self.cluster.connect()

    #TODO: Isolate all specific database bits to helper functions?
    def setUpKeyspaces(self, strategyClass = 'SimpleStrategy', opts = []):
        """
        Create the necessary keyspaces & tables with the given strategy options.
        All keyspaces use the same strategies. See the CQL documentation for
        CREATE KEYSPACE for more details.

        Args:
            strategyClass (string, optional): Defaults to 'SimpleStrategy'.
            opts: Variable length list of tuples (strategy name (string), strategy val (string)).
        """
        for kspcName in self.keyspaceNames:
            # Create the keyspace
            query = "CREATE KEYSPACE IF NOT EXISTS %(kspcName)s WITH replication = { 'class': \'%(strategyClass)s\'" % locals()
            for opt in opts:
                optName = opt[0]
                optVal = opt[1]
                query += ", \'%(optName)s\': \'%(optVal)s\'" % locals()
            query += '};'
            self.session.execute(query)
            # Create the table
            #TODO: Uid type?
            query = """CREATE TABLE IF NOT EXISTS %(kspcName)s.cmpTable (
                        uid varchar,
                        cmp int,
                        mapped boolean,
                        PRIMARY KEY (uid)
                    );""" % locals()
            #TODO: Can't use cmp list as primary key. Sort by somehow?
            #TODO: Use cluster ordering on this table?
            self.session.execute(query)

    def populateTable(self, uidGenerator):
        """
        Populate the cmpTable's with uid's, cmp numbers, and a false mapped value.

        Args:
            uidGenerator (generator): Stream the uid's to this function."""
        currentCmp = 0
        for uid in uidGenerator:
            kspcName = self.keyspaceNames[self.identifyKspc(uid)]
            query = "INSERT INTO %(kspcName)s.cmpTable (uid, cmp, mapped) VALUES (\'%(uid)s\', %(currentCmp)s, false)" % locals()
            if currentCmp % 1000 == 0:
                print query
            #Note, we use asynchronous queries for this.
            self.session.execute_async(query)
            currentCmp += 1
