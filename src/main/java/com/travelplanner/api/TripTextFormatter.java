package com.travelplanner.api;

import com.travelplanner.domain.Trip;
import com.travelplanner.domain.TravelPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for formatting Trip objects into human-readable text.
 */
public class TripTextFormatter {
    
    private static final Logger logger = LoggerFactory.getLogger(TripTextFormatter.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Converts a Trip object to a human-readable text format.
     *
     * @param trip The trip to format
     * @return A formatted string representation of the trip
     */
    public static String getTripAsText(Trip trip) {
        if (trip == null) {
            return "Trip not found";
        }
        
        StringBuilder textBuilder = new StringBuilder();
        
        // Trip header
        textBuilder.append("TRAVEL ITINERARY\n");
        textBuilder.append("================\n\n");
        
        // Trip basic information
        textBuilder.append("Trip ID: ").append(trip.tripId()).append("\n");
        textBuilder.append("User ID: ").append(trip.userId()).append("\n");
        textBuilder.append("Destination: ").append(trip.destination()).append("\n");
        
        // Date information
        textBuilder.append("Dates: ").append(formatDate(trip.startDate()))
                  .append(" to ").append(formatDate(trip.endDate())).append("\n");
        
        // Duration
        long durationDays = ChronoUnit.DAYS.between(trip.startDate(), trip.endDate());
        textBuilder.append("Duration: ").append(durationDays).append(" days\n");
        
        // Budget
        textBuilder.append("Budget: $").append(String.format("%.2f", trip.budget())).append("\n");
        
        // Status
        textBuilder.append("Status: ").append(trip.status()).append("\n\n");
        
        // Travel Plan
        TravelPlan plan = trip.generatedPlan();
        if (plan != null) {
            textBuilder.append("TRAVEL PLAN\n");
            textBuilder.append("-----------\n");
            textBuilder.append(plan.summary()).append("\n\n");
            
            textBuilder.append("Estimated Total Cost: $").append(String.format("%.2f", plan.totalEstimatedCost())).append("\n\n");
            
            // Daily Plans
            textBuilder.append("DAILY ITINERARY\n");
            textBuilder.append("---------------\n");
            
            if (plan.days() != null && !plan.days().isEmpty()) {
                for (TravelPlan.DayPlan day : plan.days()) {
                    textBuilder.append("\nDAY ").append(day.dayNumber()).append(" - ").append(day.date()).append("\n");
                    
                    // Accommodation
                    TravelPlan.Accommodation accommodation = day.accommodation();
                    if (accommodation != null) {
                        textBuilder.append("Accommodation: ").append(accommodation.name()).append("\n");
                        textBuilder.append("  ").append(accommodation.description()).append("\n");
                        textBuilder.append("  Estimated Cost: $").append(String.format("%.2f", accommodation.estimatedCost())).append("\n\n");
                    }
                    
                    // Transportation
                    if (day.transportation() != null && !day.transportation().isEmpty()) {
                        textBuilder.append("Transportation:\n");
                        for (TravelPlan.Transportation transport : day.transportation()) {
                            textBuilder.append("- ").append(transport.type()).append(": ");
                            textBuilder.append(transport.description()).append("\n");
                            textBuilder.append("  Estimated Cost: $").append(String.format("%.2f", transport.estimatedCost())).append("\n");
                        }
                        textBuilder.append("\n");
                    }
                    
                    // Activities
                    if (day.activities() != null && !day.activities().isEmpty()) {
                        textBuilder.append("Activities:\n");
                        for (TravelPlan.Activity activity : day.activities()) {
                            textBuilder.append("- ").append(activity.timeOfDay()).append(": ");
                            textBuilder.append(activity.name()).append("\n");
                            textBuilder.append("  ").append(activity.description()).append("\n");
                            textBuilder.append("  Estimated Cost: $").append(String.format("%.2f", activity.estimatedCost())).append("\n");
                        }
                        textBuilder.append("\n");
                    }
                    
                    // Meals
                    if (day.meals() != null && !day.meals().isEmpty()) {
                        textBuilder.append("Meals:\n");
                        for (TravelPlan.Meal meal : day.meals()) {
                            textBuilder.append("- ").append(meal.type()).append(": ");
                            textBuilder.append(meal.suggestion()).append("\n");
                            textBuilder.append("  Estimated Cost: $").append(String.format("%.2f", meal.estimatedCost())).append("\n");
                        }
                        textBuilder.append("\n");
                    }
                    
                    textBuilder.append("Daily Estimated Cost: $").append(String.format("%.2f", day.dailyEstimatedCost())).append("\n");
                }
            } else {
                textBuilder.append("No daily plans available\n\n");
            }
        } else {
            textBuilder.append("No travel plan generated yet\n\n");
        }
        
        // Footer
        textBuilder.append("\n================\n");
        textBuilder.append("Generated on: ").append(formatDate(LocalDate.now()));
        
        return textBuilder.toString();
    }
    
    /**
     * Formats a LocalDate into a human-readable string.
     */
    private static String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DATE_FORMATTER);
    }
}
