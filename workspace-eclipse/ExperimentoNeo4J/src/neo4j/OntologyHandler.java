package neo4j;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.UniqueFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

public class OntologyHandler {
	
	private String ontologyPath;
	private File ontologyFile;
	
	public OntologyHandler(String ontologyPath){
		this.ontologyPath = ontologyPath;
		this.ontologyFile = new File(this.ontologyPath);
	}
	
	public void importOntology (GraphDatabaseService graphDB) throws Exception{
		try {
			//Load ontology from file
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(this.ontologyFile);
			
			//Start Reasoner
			OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
			OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
			
			//Check if ontology is consistent
			if(!reasoner.isConsistent()){
				throw new Exception("Ontology is inconsistent");
			}
			
			System.out.println("Loaded ontology and it's consistent");
			
			//Start transaction in the graph db
			Transaction tx = graphDB.beginTx();
			
			try{
				//Creating default "Thing" node
				Node thingNode = getOrCreateNodeWithUniqueFactory("owl:Thing", graphDB);
				
				//Iterate over ontology classes
				Set<OWLClass> owlClasses = ontology.getClassesInSignature();
				for (OWLClass owlClass : owlClasses) {
					
					String classString = owlClass.toString();                 

					if (classString.contains("#")) {                     
						classString = classString.substring(classString.indexOf("#")+1, classString.lastIndexOf(">"));                 
					}                 
					System.out.println("Creating Node: " + classString);
					//Creating node for the OWLClass
					Node classNode = getOrCreateNodeWithUniqueFactory(classString, graphDB);
					Label nodeLabel = DynamicLabel.label(classString);
					classNode.addLabel(nodeLabel);
					
					if (classNode != null){
						System.out.println("------ Node created");
					}
					//Getting the class super classes
					NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(owlClass, true);
					if (superClasses.isEmpty()){
						//If the classe doesn't have super classes, connect it to the "Thing" node
						classNode.createRelationshipTo(thingNode, DynamicRelationshipType.withName("isA"));
					} else {
						//If not, iterate over super classes
						Set<OWLClass> superClassesSet = superClasses.getFlattened();
						for (OWLClass superClass : superClassesSet){
							String parentString = superClass.toString();

							if (parentString.contains("#")) {
								parentString = parentString.substring(parentString.indexOf("#")+1, parentString.lastIndexOf(">"));
							}

							Node parentNode = getOrCreateNodeWithUniqueFactory(parentString, graphDB);
							classNode.createRelationshipTo(parentNode, DynamicRelationshipType.withName("isA"));
						}
					}
					
				}
				
				tx.success();
			} finally {
				tx.close();
			}
		
			
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}
	
	private static Node getOrCreateNodeWithUniqueFactory(String nodeName, GraphDatabaseService graphDB) {
        UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory(graphDB, "index") {
            @Override
            protected void initialize(Node created,Map<String, Object> properties) {
                created.setProperty("name", properties.get("name"));
            }
        };

        return factory.getOrCreate("name", nodeName);
    }

}
