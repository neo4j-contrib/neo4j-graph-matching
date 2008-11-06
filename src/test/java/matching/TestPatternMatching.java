package matching;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;
import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.util.matching.PatternMatch;
import org.neo4j.util.matching.PatternMatcher;
import org.neo4j.util.matching.PatternNode;
import org.neo4j.util.matching.PatternRelationship;

public class TestPatternMatching extends TestCase
{
	private EmbeddedNeo neo;
	private Transaction tx;
	
	private static enum MyRelTypes implements RelationshipType
	{
		R1,
		R2,
		R3;
	}
	
	private Node createInstance( String name )
	{
		Node node = neo.createNode();
		node.setProperty( "name", name );
		return node;
	}
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		neo = new EmbeddedNeo( "var/neo" );
		tx = neo.beginTx();
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		tx.finish();
		neo.shutdown();
	}
	
	public TestPatternMatching( String name )
	{
		super( name );
	}
	
	private Iterable<PatternMatch> doMatch( PatternNode pNode, Node node )
	{
	    return PatternMatcher.getMatcher().match( pNode, node,
	        new HashMap<String, PatternNode>() );
	}
    
    private Iterable<PatternMatch> doMatch( PatternNode pNode, Node node,
        PatternNode... optionalNodes )
    {
        return PatternMatcher.getMatcher().match( pNode, node,
            new HashMap<String, PatternNode>(), optionalNodes );
    }
    
    public void testAllRelTypes()
    {
        final RelationshipType R1 = MyRelTypes.R1;
        final RelationshipType R2 = MyRelTypes.R2;
        
        Node a1 = createInstance( "a1" );
        Node b1 = createInstance( "b1" );

        Set<Relationship> relSet = new HashSet<Relationship>();
        relSet.add( a1.createRelationshipTo( b1, R1 ) );
        relSet.add( a1.createRelationshipTo( b1, R2 ) );

        PatternNode pA = new PatternNode();
        PatternNode pB = new PatternNode();
        PatternRelationship pRel = pA.createRelationshipTo( pB );
        int count = 0;
        for ( PatternMatch match : 
            doMatch( pA, a1 ) )
        {
            assertEquals( match.getNodeFor( pA ), a1 );
            assertEquals( match.getNodeFor( pB ), b1 );
            assertTrue( relSet.remove( match.getRelationshipFor( pRel ) ) );
            count++;
        }
        assertEquals( 0, relSet.size() );
        assertEquals( 2, count );
    }
	
    public void testAllRelTypesWithRelProperty()
    {
        final RelationshipType R1 = MyRelTypes.R1;
        final RelationshipType R2 = MyRelTypes.R2;
        
        Node a1 = createInstance( "a1" );
        Node b1 = createInstance( "b1" );

        Relationship rel = a1.createRelationshipTo( b1, R1 );
        // rel.setProperty( "musthave", true );
        rel = a1.createRelationshipTo( b1, R2 );
        rel.setProperty( "musthave", true );
        
        PatternNode pA = new PatternNode();
        PatternNode pB = new PatternNode();
        PatternRelationship pRel = pA.createRelationshipTo( pB );
        pRel.addPropertyExistConstraint( "musthave" );
        int count = 0;
        for ( PatternMatch match : 
            doMatch( pA, a1 ) )
        {
            assertEquals( match.getNodeFor( pA ), a1 );
            assertEquals( match.getNodeFor( pB ), b1 );
            count++;
        }
        assertEquals( 1, count );
    }
    
	public void testTeethStructure()
	{
		final RelationshipType R1 = MyRelTypes.R1;
		final RelationshipType R2 = MyRelTypes.R2;
		
		Node aT = createInstance( "aType" );
		Node a1 = createInstance( "a1" );
		Node bT = createInstance( "bType" );
		Node b1 = createInstance( "b1" );
		Node cT = createInstance( "cType" );
		Node c1 = createInstance( "c1" );
		Node c2 = createInstance( "c2" );
		Node dT = createInstance( "dType" );
		Node d1 = createInstance( "d1" );
		Node d2 = createInstance( "d2" );
		Node eT = createInstance( "eType" );
		Node e1 = createInstance( "e1" );

		aT.createRelationshipTo( a1, R1 );
		bT.createRelationshipTo( b1, R1 );
		cT.createRelationshipTo( c1, R1 );
		cT.createRelationshipTo( c2, R1 );
		dT.createRelationshipTo( d1, R1 );
		dT.createRelationshipTo( d2, R1 );
		eT.createRelationshipTo( e1, R1 );

		a1.createRelationshipTo( b1, R2 );
		b1.createRelationshipTo( c1, R2 );
		b1.createRelationshipTo( c2, R2 );
		c1.createRelationshipTo( d1, R2 );
		c2.createRelationshipTo( d2, R2 );
		d1.createRelationshipTo( e1, R2 );
		d2.createRelationshipTo( e1, R2 );
	
		PatternNode pA = new PatternNode();
		PatternNode pAI = new PatternNode();
		pA.createRelationshipTo( pAI, R1 );
		PatternNode pB = new PatternNode();
		PatternNode pBI = new PatternNode();
		pB.createRelationshipTo( pBI, R1 );
		PatternNode pC = new PatternNode();
		PatternNode pCI = new PatternNode();
		pC.createRelationshipTo( pCI, R1 );
		PatternNode pD = new PatternNode();
		PatternNode pDI = new PatternNode();
		pD.createRelationshipTo( pDI, R1 );
		PatternNode pE = new PatternNode();
		PatternNode pEI = new PatternNode();
		pE.createRelationshipTo( pEI, R1 );
		
		pAI.createRelationshipTo( pBI, R2 );
		pBI.createRelationshipTo( pCI, R2 );
		pCI.createRelationshipTo( pDI, R2 );
		pDI.createRelationshipTo( pEI, R2 );
		
		int count = 0;
		for ( PatternMatch match : 
			doMatch( pA, aT ) )
		{
			assertEquals( match.getNodeFor( pA ), aT );
			assertEquals( match.getNodeFor( pAI ), a1 );
			assertEquals( match.getNodeFor( pB ), bT );
			assertEquals( match.getNodeFor( pBI ), b1 );
			assertEquals( match.getNodeFor( pC ), cT );
			Node c = match.getNodeFor( pCI );
			if ( !c.equals( c1 ) && !c.equals( c2 ) )
			{
				fail( "either c1 or c2" );
			}
			assertEquals( match.getNodeFor( pD ), dT );
			Node d = match.getNodeFor( pDI );
			if ( !d.equals( d1 ) && !d.equals( d2 ) )
			{
				fail( "either d1 or d2" );
			}
			assertEquals( match.getNodeFor( pE ), eT );
			assertEquals( match.getNodeFor( pEI ), e1 );
			count++;
		}
		assertEquals( 2, count );
		
		count = 0;
		for ( PatternMatch match : 
			doMatch( pCI, c2 ) )
		{
			assertEquals( match.getNodeFor( pA ), aT );
			assertEquals( match.getNodeFor( pAI ), a1 );
			assertEquals( match.getNodeFor( pB ), bT );
			assertEquals( match.getNodeFor( pBI ), b1 );
			assertEquals( match.getNodeFor( pC ), cT );
			assertEquals( match.getNodeFor( pCI ), c2 );
			assertEquals( match.getNodeFor( pD ), dT );
			assertEquals( match.getNodeFor( pDI ), d2 );
			assertEquals( match.getNodeFor( pE ), eT );
			assertEquals( match.getNodeFor( pEI ), e1 );
			count++;
		}
		assertEquals( 1, count );		
	}
	
	public void testNonCyclicABC()
	{
		Node a = createInstance( "A" );
		Node b1 = createInstance( "B1" );
		Node b2 = createInstance( "B2" );
		Node b3 = createInstance( "B3" );
		Node c = createInstance( "C" );
		
		final RelationshipType R = MyRelTypes.R1;
		
		Relationship rAB1 = a.createRelationshipTo( b1, R );
        Relationship rAB2 = a.createRelationshipTo( b2, R );
        Relationship rAB3 = a.createRelationshipTo( b3, R );
        Relationship rB1C = b1.createRelationshipTo( c, R );
        Relationship rB2C = b2.createRelationshipTo( c, R );
        Relationship rB3C = b3.createRelationshipTo( c, R );
		
		PatternNode pA = new PatternNode();
		PatternNode pB = new PatternNode();
		PatternNode pC = new PatternNode();
		
		PatternRelationship pAB = pA.createRelationshipTo( pB, R );
		PatternRelationship pBC = pB.createRelationshipTo( pC, R );
		
		int count = 0;
		for ( PatternMatch match : 
			doMatch( pA, a ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			Node b = match.getNodeFor( pB );
			if ( !b.equals( b1 ) && !b.equals( b2 ) && !b.equals( b3 ) )
			{
				fail( "either b1 or b2 or b3" );
			}
            Relationship rB = match.getRelationshipFor( pAB );
            if ( !rAB1.equals( rB ) && !rAB2.equals( rB ) && !rAB3.equals( rB ))
            {
                fail( "either rAB1, rAB2 or rAB3" );
            }
			assertEquals( match.getNodeFor( pC ), c );
            Relationship rC = match.getRelationshipFor( pBC );
            if ( !rB1C.equals( rC ) && !rB2C.equals( rC ) && !rB3C.equals( rC ))
            {
                fail( "either rB1C, rB2C or rB3C" );
            }
			count++;
		}
		assertEquals( 3, count );
		count = 0;
		for ( PatternMatch match : 
			doMatch( pB, b2 ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			assertEquals( match.getNodeFor( pB ), b2 );
			assertEquals( match.getNodeFor( pC ), c );
			count++;
		}
		assertEquals( 1, count );
	}
	
	public void testCyclicABC()
	{
		Node a = createInstance( "A" );
		Node b1 = createInstance( "B1" );
		Node b2 = createInstance( "B2" );
		Node b3 = createInstance( "B3" );
		Node c = createInstance( "C" );
		
		final RelationshipType R = MyRelTypes.R1;
		
		a.createRelationshipTo( b1, R );
		a.createRelationshipTo( b2, R );
		a.createRelationshipTo( b3, R );
		b1.createRelationshipTo( c, R );
		b2.createRelationshipTo( c, R );
		b3.createRelationshipTo( c, R );
		c.createRelationshipTo( a, R );
		
		PatternNode pA = new PatternNode();
		PatternNode pB = new PatternNode();
		PatternNode pC = new PatternNode();
		
		pA.createRelationshipTo( pB, R );
		pB.createRelationshipTo( pC, R );
		pC.createRelationshipTo( pA, R );
		
		int count = 0;
		for ( PatternMatch match : 
			doMatch( pA, a ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			Node b = match.getNodeFor( pB );
			if ( !b.equals( b1 ) && !b.equals( b2 ) && !b.equals( b3 ) )
			{
				fail( "either b1 or b2 or b3" );
			}
			assertEquals( match.getNodeFor( pC ), c );
			count++;
		}
		assertEquals( 3, count );
		count = 0;
		for ( PatternMatch match : 
			doMatch( pB, b2 ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			Node b = match.getNodeFor( pB );
			if ( !b.equals( b1 ) && !b.equals( b2 ) && !b.equals( b3 ) )
			{
				fail( "either b1 or b2 or b3" );
			}
			assertEquals( match.getNodeFor( pC ), c );
			count++;
		}
		assertEquals( 3, count );
	}

	public void testPropertyABC()
	{
		Node a = createInstance( "A" );
		a.setProperty( "hasProperty", true );
		Node b1 = createInstance( "B1" );
		b1.setProperty( "equals", 1 );
		Node b2 = createInstance( "B2" );
		b2.setProperty( "equals", 1 );
		Node b3 = createInstance( "B3" );
		b3.setProperty( "equals", 2 );
		Node c = createInstance( "C" );
		
		final RelationshipType R = MyRelTypes.R1;
		
		a.createRelationshipTo( b1, R );
		a.createRelationshipTo( b2, R );
		a.createRelationshipTo( b3, R );
		b1.createRelationshipTo( c, R );
		b2.createRelationshipTo( c, R );
		b3.createRelationshipTo( c, R );
		
		PatternNode pA = new PatternNode();
		pA.addPropertyExistConstraint( "hasProperty" );
		PatternNode pB = new PatternNode();
		pB.addPropertyEqualConstraint( "equals", 1 );
		PatternNode pC = new PatternNode();
		
		pA.createRelationshipTo( pB, R );
		pB.createRelationshipTo( pC, R );
		
		int count = 0;
		for ( PatternMatch match : 
			doMatch( pA, a ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			Node b = match.getNodeFor( pB );
			if ( !b.equals( b1 ) && !b.equals( b2 ) )
			{
				fail( "either b1 or b2" );
			}
			assertEquals( match.getNodeFor( pC ), c );
			count++;
		}
		assertEquals( 2, count );
		count = 0;
		for ( PatternMatch match : 
			doMatch( pB, b2 ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			assertEquals( match.getNodeFor( pB ), b2 );
			assertEquals( match.getNodeFor( pC ), c );
			count++;
		}
		assertEquals( 1, count );
	}
	
	public void testOptional()
	{
		Node a = createInstance( "A" );
		Node b1 = createInstance( "B1" );
		Node b2 = createInstance( "B2" );
		Node c = createInstance( "C" );
		Node d1 = createInstance( "D1" );
		Node d2 = createInstance( "D2" );
		Node e1 = createInstance( "E1" );
		Node e2 = createInstance( "E2" );
		Node f1 = createInstance( "F1" );
		Node f2 = createInstance( "F2" );
		Node f3 = createInstance( "F3" );
		
		final RelationshipType R1 = MyRelTypes.R1;
		final RelationshipType R2 = MyRelTypes.R2;
		final RelationshipType R3 = MyRelTypes.R3;
		a.createRelationshipTo( b1, R1 );
		a.createRelationshipTo( b2, R1 );
		a.createRelationshipTo( c, R2 );
		a.createRelationshipTo( f1, R3 );
		a.createRelationshipTo( f2, R3 );
		a.createRelationshipTo( f3, R3 );
		c.createRelationshipTo( d1, R1 );
		c.createRelationshipTo( d2, R1 );
		d1.createRelationshipTo( e1, R2 );
		d1.createRelationshipTo( e2, R2 );
		
		// Required part of the graph
		PatternNode pA = new PatternNode( "pA" );
		PatternNode pC = new PatternNode( "pC" );
		pA.createRelationshipTo( pC, R2 );

		// First optional branch
		PatternNode oA1 = new PatternNode( "pA" );
		PatternNode oB1 = new PatternNode( "pB" );
		oA1.createRelationshipTo( oB1, R1, true );

//		// Second optional branch
		PatternNode oA2 = new PatternNode( "pA" );
		PatternNode oF2 = new PatternNode( "pF" );
		oA2.createRelationshipTo( oF2, R3, true );

		// Third optional branch
		PatternNode oC3 = new PatternNode( "pC" );
		PatternNode oD3 = new PatternNode( "pD" );
		PatternNode oE3 = new PatternNode( "pE" );
		oC3.createRelationshipTo( oD3, R1, true );
		oD3.createRelationshipTo( oE3, R2, true );

		// Test that all permutations are there and that multiple optional
		// branches work.
		int count = 0;
		for ( PatternMatch match :
			doMatch( pA, a, oA1, oA2, oC3 ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			Node bMatch = match.getNodeFor( oB1 );
			if ( !bMatch.equals( b1 ) && !bMatch.equals( b2 ) )
			{
				fail( "either b1 or b2" );
			}
			Node fMatch = match.getNodeFor( oF2 );
			if ( !fMatch.equals( f1 ) && !fMatch.equals( f2 ) &&
				!fMatch.equals( f3 ) )
			{
				fail( "either f1, f2 or f3" );
			}
			assertEquals( match.getNodeFor( pC ), c );
			assertEquals( match.getNodeFor( oD3 ), d1 );
			Node eMatch = match.getNodeFor( oE3 );
			assertTrue( eMatch.equals( e1 ) || eMatch.equals( e2 ) );
			count++;
		}
		assertEquals( count, 12 );
		
		// Test that unmatched optional branches are ignored.
		PatternNode pI = new PatternNode( "pI" );
		PatternNode pJ = new PatternNode( "pJ" );
		PatternNode pK = new PatternNode( "pK" );
		PatternNode pL = new PatternNode( "pL" );
		
		pI.createRelationshipTo( pJ, R1, true );
		pI.createRelationshipTo( pK, R2 );
		pK.createRelationshipTo( pL, R2, true );
		
		count = 0;
		for ( PatternMatch match :
			doMatch( pI, a, pI, pK ) )
		{
			assertEquals( match.getNodeFor( pI), a );
			Node jMatch = match.getNodeFor( pJ );
			if ( !jMatch.equals( b1 ) && !jMatch.equals( b2 ) )
			{
				fail( "either b1 or b2" );
			}
			assertEquals( match.getNodeFor( pK ), c );
			assertEquals( match.getNodeFor( pL ), null );
			count++;
		}
		assertEquals( count, 2 );
	}

	public void testOptional2()
	{
		Node a = createInstance( "A" );
		Node b1 = createInstance( "B1" );
		Node b2 = createInstance( "B2" );
		Node b3 = createInstance( "B3" );
		Node c1 = createInstance( "C1" );
		Node c3 = createInstance( "C3" );
		
		final RelationshipType R1 = MyRelTypes.R1;
		final RelationshipType R2 = MyRelTypes.R2;
		a.createRelationshipTo( b1, R1 );
		a.createRelationshipTo( b2, R1 );
		a.createRelationshipTo( b3, R1 );
		b1.createRelationshipTo( c1, R2 );
		b3.createRelationshipTo( c3, R2 );
		
		// Required part of the graph
		PatternNode pA = new PatternNode( "pA" );
		PatternNode pB = new PatternNode( "pB" );
		pA.createRelationshipTo( pB, R1 );

		// Optional part of the graph
		PatternNode oB = new PatternNode( "pB" );
		PatternNode oC = new PatternNode( "oC" );
		oB.createRelationshipTo( oC, R2, true );
		
		int count = 0;
		for ( PatternMatch match :
			doMatch( pA, a, oB ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			Node bMatch = match.getNodeFor( pB );
			Node optionalBMatch = match.getNodeFor( oB );
			Node optionalCMatch = match.getNodeFor( oC );
			if ( !bMatch.equals( b1 ) && !bMatch.equals( b2 ) &&
				!bMatch.equals( b3 ) )
			{
				fail( "either b1, b2 or b3" );
			}
			if ( optionalBMatch != null )
			{
				assertEquals( bMatch, optionalBMatch );
				if ( optionalBMatch.equals( b1 ) )
				{
					assertEquals( optionalCMatch, c1 );
				}
				else if ( optionalBMatch.equals( b3 ) )
				{
					assertEquals( optionalCMatch, c3 );
				}
				else
				{
					assertEquals( optionalCMatch, null );
				}
			}
			count++;
		}
		assertEquals( count, 3 );
	}

	public void testArrayPropertyValues()
	{
		Node a = createInstance( "A" );
		a.setProperty( "hasProperty", true );
		Node b1 = createInstance( "B1" );
		b1.setProperty( "equals", new Integer[] { 19, 1 } );
		Node b2 = createInstance( "B2" );
		b2.setProperty( "equals", new Integer[] { 1, 10, 12 } );
		Node b3 = createInstance( "B3" );
		b3.setProperty( "equals", 2 );
		Node c = createInstance( "C" );
		
		final RelationshipType R = MyRelTypes.R1;
		
		a.createRelationshipTo( b1, R );
		a.createRelationshipTo( b2, R );
		a.createRelationshipTo( b3, R );
		b1.createRelationshipTo( c, R );
		b2.createRelationshipTo( c, R );
		b3.createRelationshipTo( c, R );
		
		PatternNode pA = new PatternNode();
		pA.addPropertyExistConstraint( "hasProperty" );
		PatternNode pB = new PatternNode();
		pB.addPropertyEqualConstraint( "equals", 1 );
		PatternNode pC = new PatternNode();
		
		pA.createRelationshipTo( pB, R );
		pB.createRelationshipTo( pC, R );
		
		int count = 0;
		for ( PatternMatch match : 
			doMatch( pA, a ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			Node b = match.getNodeFor( pB );
			if ( !b.equals( b1 ) && !b.equals( b2 ) )
			{
				fail( "either b1 or b2" );
			}
			assertEquals( match.getNodeFor( pC ), c );
			count++;
		}
		assertEquals( 2, count );
		count = 0;
		for ( PatternMatch match : 
			doMatch( pB, b2 ) )
		{
			assertEquals( match.getNodeFor( pA ), a );
			assertEquals( match.getNodeFor( pB ), b2 );
			assertEquals( match.getNodeFor( pC ), c );
			count++;
		}
		assertEquals( 1, count );
	}
}
