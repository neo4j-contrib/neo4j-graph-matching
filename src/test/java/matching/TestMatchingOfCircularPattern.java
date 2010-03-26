package matching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.commons.iterator.IteratorWrapper;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.graphmatching.PatternMatch;
import org.neo4j.graphmatching.PatternMatcher;
import org.neo4j.graphmatching.PatternNode;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class TestMatchingOfCircularPattern
{
    private static class VisibleMessagesByFollowedUsers implements
            Iterable<Node>
    {
        private static final PatternNode start = new PatternNode();
        private static final PatternNode message = new PatternNode();
        static
        {
            PatternNode user = new PatternNode();
            start.createRelationshipTo( user, withName( "FOLLOWS" ) );
            user.createRelationshipTo( message, withName( "CREATED" ) );
            message.createRelationshipTo( start, withName( "IS_VISIBLE_BY" ) );
        }

        private final Node startNode;

        public VisibleMessagesByFollowedUsers( Node startNode )
        {
            this.startNode = startNode;
        }

        public Iterator<Node> iterator()
        {
            Iterable<PatternMatch> matches = PatternMatcher.getMatcher().match(
                    start, startNode );
            return new IteratorWrapper<Node, PatternMatch>( matches.iterator() )
            {
                @Override
                protected Node underlyingObjectToObject( PatternMatch match )
                {
                    return match.getNodeFor( message );
                }
            };
        }
    }

    private static final int EXPECTED_VISIBLE_MESSAGE_COUNT = 3;
    private static Node user;

    public static void setupGraph()
    {
        user = graphdb.createNode();
        Node user1 = graphdb.createNode(), user2 = graphdb.createNode(), user3 = graphdb.createNode();
        user.createRelationshipTo( user1, withName( "FOLLOWS" ) );
        user1.createRelationshipTo( user3, withName( "FOLLOWS" ) );
        user.createRelationshipTo( user2, withName( "FOLLOWS" ) );
        createMessage( user, "invisible", user1, user2 );
        createMessage( user1, "visible", user, user2, user3 );
        createMessage( user1, "visible", user );
        createMessage( user2, "visible", user, user1 );
        createMessage( user2, "invisible", user1, user3 );
        createMessage( user3, "invisible", user1, user2 );
        createMessage( user3, "invisible", user );
    }

    private static void createMessage( Node creator, String text,
            Node... visibleBy )
    {
        Node message = graphdb.createNode();
        message.setProperty( "text", text );
        creator.createRelationshipTo( message, withName( "CREATED" ) );
        for ( Node user : visibleBy )
        {
            message.createRelationshipTo( user, withName( "IS_VISIBLE_BY" ) );
        }
    }

    @Test
    public void messageNodesAreOnlyReturnedOnce()
    {
        Map<Node, Integer> counts = new HashMap<Node, Integer>();
        for ( Node message : new VisibleMessagesByFollowedUsers( user ) )
        {
            Integer seen = counts.get( message );
            counts.put( message, seen == null ? 1 : ( seen + 1 ) );
            count++;
        }
        StringBuilder duplicates = null;
        for ( Map.Entry<Node, Integer> seen : counts.entrySet() )
        {
            if ( seen.getValue() > 1 )
            {
                if ( duplicates == null )
                {
                    duplicates = new StringBuilder(
                            "These nodes occured multiple times (expected once): " );
                }
                else
                {
                    duplicates.append( ", " );
                }
                duplicates.append( seen.getKey() );
                duplicates.append( " (" );
                duplicates.append( seen.getValue() );
                duplicates.append( " times)" );
            }
        }
        if ( duplicates != null )
        {
            fail( duplicates.toString() );
        }
        tx.success();
    }

    @Test
    public void canFindMessageNodesThroughGraphMatching()
    {
        for ( Node message : new VisibleMessagesByFollowedUsers( user ) )
        {
            verifyMessage( message );
        }
        tx.success();
    }

    @Test
    public void canFindMessageNodesThroughTraversing()
    {
        for ( Node message : traverse( user ) )
        {
            verifyMessage( message );
        }
        tx.success();
    }

    private void verifyMessage( Node message )
    {
        assertNotNull( message );
        assertEquals( "visible", message.getProperty( "text", null ) );
        count++;
    }

    private int count;
    private Transaction tx;

    @Before
    public void resetCount()
    {
        count = 0;
        tx = graphdb.beginTx();
    }

    @After
    public void verifyCount()
    {
        tx.finish();
        tx = null;
        assertEquals( EXPECTED_VISIBLE_MESSAGE_COUNT, count );
    }

    private static Iterable<Node> traverse( final Node startNode )
    {
        return startNode.traverse( Order.BREADTH_FIRST, stopAtDepth( 2 ),
                new ReturnableEvaluator()
                {
                    public boolean isReturnableNode( TraversalPosition pos )
                    {
                        Node node = pos.currentNode();
                        return isMessage( node )
                               && isVisibleTo( node, startNode );
                    }
                }, withName( "FOLLOWS" ), Direction.OUTGOING,
                withName( "CREATED" ), Direction.OUTGOING );
    }

    public static StopEvaluator stopAtDepth( final int depth )
    {
        return new StopEvaluator()
        {
            public boolean isStopNode( TraversalPosition currentPos )
            {
                return currentPos.depth() >= depth;
            }
        };
    }

    static boolean isMessage( Node node )
    {
        return node.hasProperty( "text" );
    }

    static boolean isVisibleTo( Node message, Node user )
    {
        for ( Relationship visibility : message.getRelationships(
                withName( "IS_VISIBLE_BY" ), Direction.OUTGOING ) )
        {
            if ( visibility.getEndNode().equals( user ) )
            {
                return true;
            }
        }
        return false;
    }

    private static GraphDatabaseService graphdb;

    @BeforeClass
    public static void setUpDb()
    {
        graphdb = new EmbeddedGraphDatabase( "target/var/db" );
        Transaction tx = graphdb.beginTx();
        try
        {
            setupGraph();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @AfterClass
    public static void stopGraphdb()
    {
        graphdb.shutdown();
        graphdb = null;
    }
}
