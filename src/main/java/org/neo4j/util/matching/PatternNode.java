package org.neo4j.util.matching;

import java.util.LinkedList;
import org.neo4j.api.core.RelationshipType;

public class PatternNode
{
	 private LinkedList<PatternRelationship> relationships = 
		new LinkedList<PatternRelationship>();
	 
	 private String propertyName = null;
	 private Object propertyValue = null;
	 private String label;
	
	public PatternNode()
	{
		this.label = "";
	}
	
	public PatternNode( String label )
	{
		this.label = label;
	}
	
	public Iterable<PatternRelationship> getRelationships()
	{
		return relationships;
	}
	
	void addRelationship( PatternRelationship relationship )
	{
		relationships.add( relationship );
	}
	
	void removeRelationship( PatternRelationship relationship )
	{
		relationships.remove( relationship );
	}
	
	public PatternRelationship createRelationshipTo( 
		PatternNode otherNode, RelationshipType type )
	{
		PatternRelationship relationship = 
			new PatternRelationship( type, this, otherNode );
		addRelationship( relationship );
		otherNode.addRelationship( relationship );
		return relationship;
	}
	
	public void setPropertyExistConstraint( String propertyName )
	{
		this.propertyName = propertyName;
		propertyValue = null;
	}
	
	public void setPropertyEqualConstraint( String propertyName, Object value )
	{
		this.propertyName = propertyName;
		this.propertyValue = value;
	}
	
	public String getLabel()
	{
		return this.label;
	}
	
	@Override
	public String toString()
	{
		return this.label;
	}
	
	String getPropertyName()
	{
		return propertyName;
	}
	
	Object getPropertyValue()
	{
		return propertyValue;
	}
}
