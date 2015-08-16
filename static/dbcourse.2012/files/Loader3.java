import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Loader3 extends TestCase {

    private static final Logger log = Logger.getLogger(Loader3.class);

    public static final Pattern TAB_SPLITTER = Pattern.compile("\t");
    public static final String PATH = "/Users/pulser/mine/neo/";

    EmbeddedGraphDatabase graphDb = null;
    Transaction transaction = null;

    public void setUp() throws Exception {
        super.setUp();
        if (graphDb == null) {
            graphDb = new EmbeddedGraphDatabase("/Users/pulser/neo4j/imdb/graph.db");
            transaction = graphDb.beginTx();
        }
    }

    protected void tearDown() throws Exception {
        transaction.success();
        transaction.finish();
        graphDb.shutdown();
        super.tearDown();
    }

    private <K> List<K> loadFile(final String name, final Fu<String[], K> reader) throws IOException {
        final List<K> out = Cf.newList();
        final BufferedReader in = new BufferedReader(new FileReader(PATH + name));
        String line = in.readLine(); // skip first line
        while ((line = in.readLine()) != null) {
            out.add(reader.apply(TAB_SPLITTER.split(line)));
        }
        return out;
    }

    private static enum RelTypes implements RelationshipType {
        act
    }

    public void testSearch() throws Exception {
        for (final Node node : graphDb.getAllNodes()) {
            if (node.hasProperty("name")) {
                final String name = node.getProperty("name").toString();
                if (name.contains("Clooney")) {
                    System.out.println(node.getId() + " name = " + name);
                }
            }
        }
    }

    static class MovieActor {
        public final String movieID;
        public final String actorID;
        public final String actorName;
        public final int ranking;

        MovieActor(final String[] items) {
            this.movieID = items[0];
            this.actorID = items[1];
            this.actorName = items[2];
            this.ranking = items[3] != null ? Integer.valueOf(items[3]) : -1;
        }

        public static Fu<String[], MovieActor> getReader() {
            return new Fu<String[], MovieActor>() {
                @Override
                public MovieActor apply(final String[] arg) {
                    return new MovieActor(arg);
                }
            };
        }

        @Override
        public String toString() {
            return "MovieActor{" +
                    "movieID='" + movieID + '\'' +
                    ", actorID='" + actorID + '\'' +
                    ", actorName='" + actorName + '\'' +
                    ", ranking=" + ranking +
                    '}';
        }
    }

    static class Movie {
        //0 - id
        public final int id;
        //1 - title
        public final String title;
        //2 - imdbID
        public final int imdbId;
        //3 - spanishTitle
//4 - imdbPictureURL
//5 - year
        public final int year;

        //rtID
//rtAllCriticsRating
//rtAllCriticsNumReviews
//rtAllCriticsNumFresh
//rtAllCriticsNumRotten
//rtAllCriticsScore
//rtTopCriticsRating
//rtTopCriticsNumReviews
//rtTopCriticsNumFresh
//rtTopCriticsNumRotten
//rtTopCriticsScore
//rtAudienceRating
//rtAudienceNumRatings
//rtAudienceScore
//rtPictureURL
        Movie(final String[] items) {
            this.id = Integer.valueOf(items[0]);
            this.title = items[1];
            this.imdbId = Integer.valueOf(items[2]);
            this.year = Integer.valueOf(items[5]);
        }

        public static Fu<String[], Movie> getReader() {
            return new Fu<String[], Movie>() {
                @Override
                public Movie apply(final String[] arg) {
                    return new Movie(arg);
                }
            };
        }

        @Override
        public String toString() {
            return "Movie{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    ", imdbId=" + imdbId +
                    ", year=" + year +
                    '}';
        }
    }

    public void testLoadActors2012() throws Exception {
        System.out.println("out = " + loadFile("mine_movie_actor.dat", MovieActor.getReader()).size());
    }

    public void testMovies2012() throws Exception {
        System.out.println("out = " + loadFile("mine_movies.dat", Movie.getReader()).size());
    }

    public void testLoadMoviesToNeo() throws Exception {

        log.info("Clear old data ... ");
        for (final Node node : graphDb.getAllNodes()) {
            for (final Relationship item : node.getRelationships()) {
                item.delete();
            }
            node.delete();
        }

        log.info("Load movies ...");
        final List<Movie> movies = loadFile("mine_movies.dat", Movie.getReader());
        final Map<String, Integer> uniqMovie = new HashMap<String, Integer>();
        for (final Movie movie : movies) {
            final String key = movie.title + "#" + movie.year;
            if (uniqMovie.containsKey(key)) {
                continue;
            }
            uniqMovie.put(key, movie.id);
        }

        final Map<Integer, Integer> movieIdToUniqMovieId = new HashMap<Integer, Integer>();
        for (final Movie movie : movies) {
            final String key = movie.title + "#" + movie.year;
            final Integer uniq = uniqMovie.get(key);
            movieIdToUniqMovieId.put(movie.id, uniq);
        }

        final Map<Integer, Node> movieToNodeMapping = new HashMap<Integer, Node>();
        log.info("Create nodes for movies ...");
        for (final Movie movie : movies) {

            if (movie.id != movieIdToUniqMovieId.get(movie.id)) {
                continue;
            }

            final Node v = graphDb.createNode();
            v.setProperty("type", "movie");
            v.setProperty("title", movie.title);
            v.setProperty("src-id", movie.id);
            v.setProperty("imdbId", movie.imdbId);
            v.setProperty("year", movie.year);

            movieToNodeMapping.put(movie.id, v);

        }

        log.info("Load actors ...");
        final List<MovieActor> actors = loadFile("mine_movie_actor.dat", MovieActor.getReader());
        final Map<String, String> uniqActors = new HashMap<String, String>();
        for (final MovieActor actor : actors) {
            uniqActors.put(actor.actorID, actor.actorName);
        }

        log.info("Create nodes for actors ...");
        final Map<String, Node> actorMapping = new HashMap<String, Node>();
        for (final Map.Entry<String, String> actor : uniqActors.entrySet()) {
            final Node node = graphDb.createNode();
            node.setProperty("type", "actor");
            node.setProperty("name", actor.getValue());
            node.setProperty("actor-id", actor.getKey());

            actorMapping.put(actor.getKey(), node);
        }


        log.info("Create relations ...");
        final Set<String> alreadyRegistered = new HashSet<String>();
        for (final MovieActor actor : actors) {

            final Integer mid = Integer.valueOf(actor.movieID);
            final Integer uniqMovieId = movieIdToUniqMovieId.get(mid);
            final String ukey = actor.actorID + "#" + uniqMovieId;
            if (alreadyRegistered.contains(ukey)) {
                continue;
            }

            final Node nodeMovie = movieToNodeMapping.get(uniqMovieId);
            final Node nodeActor = actorMapping.get(actor.actorID);

            if (nodeMovie == null || nodeActor == null) {
                System.out.println("actor = " + actor);
                throw new IllegalStateException();
            }

            final Relationship edge = nodeMovie.createRelationshipTo(nodeActor, RelTypes.act);
            edge.setProperty("rank", actor.ranking);

            alreadyRegistered.add(ukey);

        }

        log.info("Done.");

    }

}

