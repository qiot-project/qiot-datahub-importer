package io.qiot.covid19.datahub.importer.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

import io.qiot.covid19.datahub.importer.domain.dto.HistoricalDataPeriod;
import io.qiot.covid19.datahub.importer.domain.dto.TelemetryImportUploadResult;
import io.qiot.covid19.datahub.importer.exceptions.DataServiceException;

/**
 * The Class AbstractTelemetryService.
 *
 * @author andreabattaglia
 */
public abstract class AbstractTelemetryService
        implements TelemetryService {
    
    /** The df. */
    protected final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd",
            Locale.ENGLISH);

    /** The separator. */
    protected final String SEPARATOR = ",";

    /** The base url. */
    @ConfigProperty(name = "app.aqicn.telemetry.baseurl")
    protected String baseUrl;

    /** The token. */
    @ConfigProperty(name = "app.aqicn.token")
    protected String token;

    /** The logger. */
    @Inject
    Logger LOGGER;

    /**
     * Import all available telemetry.
     *
     * @return the list
     * @throws DataServiceException the data service exception
     */
    @Override
    public List<TelemetryImportUploadResult> importAllAvailableTelemetry()
            throws DataServiceException {
        List<TelemetryImportUploadResult> results = new ArrayList<>();
        for (HistoricalDataPeriod period : HistoricalDataPeriod.values())
            results.add(importSingleTelemetry(period));
        LOGGER.info("Import phase for ALL periods completed");
        return results;
    }

    /**
     * Import single telemetry.
     *
     * @param period the period
     * @return the telemetry import upload result
     * @throws DataServiceException the data service exception
     */
    @Override
    public TelemetryImportUploadResult importSingleTelemetry(
            HistoricalDataPeriod period) throws DataServiceException {
        URL website;
        try {
            String onlineSourceURL = baseUrl + "/" + token + "/"
                    + period.getPeriod();
            LOGGER.info("Importing raw telemetry from source {}",
                    onlineSourceURL);
            website = new URL(onlineSourceURL);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(website.openStream()));) {
                TelemetryImportUploadResult importResult = importTelemetryHistory(
                        br);
                importResult.duplicates = removeDuplicates();
                LOGGER.info("Import phase for period {} completed",
                        period.name());
                return importResult;
            } catch (IOException e) {
                throw new DataServiceException(
                        "An error occurred reading the historical data source",
                        e);
            } catch (DataServiceException e) {
                throw new DataServiceException(
                        "An error occurred importing the historical data source",
                        e);
            }
        } catch (MalformedURLException e) {
            throw new DataServiceException(
                    "An error occurred connecting to the historical data source",
                    e);
        }
    }

    /**
     * Import telemetry history.
     *
     * @param br the br
     * @return the telemetry import upload result
     * @throws DataServiceException the data service exception
     */
    @Transactional
    protected abstract TelemetryImportUploadResult importTelemetryHistory(
            BufferedReader br) throws DataServiceException;

    /**
     * Removes the duplicates.
     *
     * @return the int
     */
    @Transactional
    protected abstract int removeDuplicates();
}
