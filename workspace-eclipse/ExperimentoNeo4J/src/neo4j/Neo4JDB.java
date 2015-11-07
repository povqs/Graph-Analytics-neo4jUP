package neo4j;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class Neo4JDB {
	
	private static final String DB_PATH = "/Users/nicolle/Documents/Neo4j";
	private static final String OWL_PATH = "/Users/nicolle/Documents/UFPE/Mestrado/Experimento Neo4J/ontologyTest.owl";
	private static GraphDatabaseService graphDB;
	
	public static void main(String[] args) {
		graphDB =  new GraphDatabaseFactory()
						.newEmbeddedDatabaseBuilder(new File(DB_PATH))
						.loadPropertiesFromFile("/Users/nicolle/Documents/Neo4j/default.graphdb/neo4j.properties")
						.newGraphDatabase();
		
		registerShutdownHook( graphDB );
		
		if (graphDB != null){
			System.out.println("GRAPH DB STARTED");
			
			try {
				OntologyHandler handler = new OntologyHandler(OWL_PATH);
				handler.importOntology(graphDB);
				System.out.println("FINISHED LOADING ONTOLOGY");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}

	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
	    // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running application).
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        @Override
	        public void run()
	        {
	            graphDb.shutdown();
	        }
	    } );
	}
}
