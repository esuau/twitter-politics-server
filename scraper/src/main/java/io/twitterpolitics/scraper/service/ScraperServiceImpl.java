package io.twitterpolitics.scraper.service;

import io.twitterpolitics.repository.StatusRepository;
import io.twitterpolitics.repository.TrendRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import twitter4j.*;

@Service
public class ScraperServiceImpl implements ScraperService {

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private TrendRepository trendRepository;

    /**
     * The instance of Twitter API.
     */
    private Twitter twitter = new TwitterFactory().getInstance();

    /**
     * The instance of Twitter Stream.
     */
    private TwitterStream twitterStream = new TwitterStreamFactory().getInstance();

    /**
     * The list of current trends in France.
     */
    private Trends trends;

    @Override
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void getTrends() throws TwitterException {
        trends = twitter.getPlaceTrends(23424819);
        for (Trend trend : trends.getTrends()) {
            if (!trendRepository.checkTrendExists(trend.getName())) {
                io.twitterpolitics.entity.Trend scraperTrend = new io.twitterpolitics.entity.Trend();
                scraperTrend.setName(trend.getName());
                trendRepository.save(scraperTrend);
            }
        }
    }

    @Override
    public void saveStatuses() throws TwitterException {
        FilterQuery tweetFilterQuery = new FilterQuery();
        tweetFilterQuery.track(getTrendQueries());
        tweetFilterQuery.language("fr");
        twitterStream.addListener(new StatusListener() {
            @Override
            @Transactional
            public void onStatus(Status status) {
                if (!status.isRetweet()) {
                    io.twitterpolitics.entity.Status scraperStatus = new io.twitterpolitics.entity.Status();
                    scraperStatus.setId(status.getId());
                    scraperStatus.setCreatedAt(status.getCreatedAt());
                    scraperStatus.setText(status.getText());
                    io.twitterpolitics.entity.User scraperUser = new io.twitterpolitics.entity.User();
                    scraperUser.setId(status.getUser().getId());
                    scraperUser.setName(status.getUser().getName());
                    scraperUser.setScreenName(status.getUser().getScreenName());
                    scraperUser.setProfileImageUrl(status.getUser().getProfileImageURL());
                    scraperStatus.setUser(scraperUser);
                    statusRepository.save(scraperStatus);
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

            }

            @Override
            public void onTrackLimitationNotice(int i) {

            }

            @Override
            public void onScrubGeo(long l, long l1) {

            }

            @Override
            public void onStallWarning(StallWarning stallWarning) {

            }

            @Override
            public void onException(Exception e) {

            }
        });
        twitterStream.filter(tweetFilterQuery);
    }

    /**
     * Gets the queries corresponding to current trends for tweet filter.
     *
     * @return an array of the current trends' queries.
     */
    private String[] getTrendQueries() throws TwitterException {
        if (trends == null) getTrends();
        String[] queries = new String[trends.getTrends().length];
        for (int i = 0; i < trends.getTrends().length; i++) {
            queries[i] = trends.getTrends()[i].getQuery();
        }
        return queries;
    }

}
