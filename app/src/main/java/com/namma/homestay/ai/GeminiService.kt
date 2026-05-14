package com.namma.homestay.ai

import android.graphics.BitmapFactory
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.JsonParser
import com.namma.homestay.models.Dish
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {

    private const val API_KEY = "YOUR_GEMINI_API_KEY_HERE"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash-001",
        apiKey = API_KEY
    )

    // Generate a beautiful listing description based on inputs
    suspend fun generateDescription(
        homestayName: String,
        location: String,
        amenities: List<String>,
        nearbyAttractions: List<String>
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildString {
                    append("Write an enchanting, warm, and inviting description for a homestay called '$homestayName' ")
                    append("located in $location, Karnataka, India. ")
                    if (amenities.isNotEmpty()) {
                        append("Amenities include: ${amenities.joinToString(", ")}. ")
                    }
                    if (nearbyAttractions.isNotEmpty()) {
                        append("Nearby attractions include: ${nearbyAttractions.joinToString(", ")}. ")
                    }
                    append("The description should be 2-3 paragraphs, highlight the authentic rural Karnataka experience, ")
                    append("mention the warmth of the host family, local Malnad/Coorg cuisine, and the natural beauty. ")
                    append("Use markdown formatting with **bold** for emphasis. Keep it under 300 words.")
                }

                val response = generativeModel.generateContent(prompt)
                Result.success(response.text ?: "No description generated.")
            } catch (e: Exception) {
                Log.e("GeminiService", "generateDescription failed", e)
                Result.failure(e)
            }
        }
    }

    // Analyze a food photo and identify the dish
    suspend fun analyzeFoodPhoto(imageBytes: ByteArray): Result<FoodAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap == null) {
                    return@withContext Result.failure(Exception("Failed to decode image"))
                }

                val inputContent = content {
                    image(bitmap)
                    text(
                        "Identify this South Indian Karnataka homestay dish. " +
                        "Return ONLY a JSON object with these exact keys: " +
                        "dishName (string), description (string - a short appetizing description in 2 sentences), " +
                        "category (string - one of: Breakfast, Lunch, Snacks, Dinner), " +
                        "isVeg (boolean), isSpicy (boolean), " +
                        "suggestedPrice (number - approximate price in INR for a homestay meal). " +
                        "No markdown, no explanation, just the JSON."
                    )
                }

                val response = generativeModel.generateContent(inputContent)
                val text = response.text ?: return@withContext Result.failure(Exception("Empty response"))

                // Parse the JSON response
                val cleaned = text.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                // Simple manual parsing for reliability
                val dishName = extractJsonValue(cleaned, "dishName") ?: "Unknown Dish"
                val description = extractJsonValue(cleaned, "description") ?: "A delicious homestyle dish."
                val category = extractJsonValue(cleaned, "category") ?: "Lunch"
                val isVeg = extractJsonBoolean(cleaned, "isVeg") ?: true
                val isSpicy = extractJsonBoolean(cleaned, "isSpicy") ?: false
                val suggestedPrice = extractJsonDouble(cleaned, "suggestedPrice") ?: 50.0

                Result.success(
                    FoodAnalysisResult(
                        dishName = dishName,
                        description = description,
                        category = category,
                        isVeg = isVeg,
                        isSpicy = isSpicy,
                        suggestedPrice = suggestedPrice
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Generate dish description from name
    suspend fun generateDishDescription(dishName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Write a short, appetizing 2-sentence description for '$dishName', " +
                        "a traditional Karnataka homestay dish. Make it warm and inviting."
                val response = generativeModel.generateContent(prompt)
                Result.success(response.text ?: "A delicious homemade specialty.")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Generate nearby attraction description
    suspend fun generateAttractionDescription(spotName: String, distance: Double): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Write a short 1-sentence description for '$spotName', " +
                        "a hidden gem tourist spot ${distance}km from a homestay in Karnataka, India. " +
                        "Make it sound exciting and mysterious."
                val response = generativeModel.generateContent(prompt)
                Result.success(response.text ?: "A beautiful hidden spot nearby.")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Extract dish info from voice transcription text
    suspend fun extractDishesFromVoice(voiceText: String, category: String): Result<List<Dish>> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "A Karnataka homestay host said: \"$voiceText\". " +
                    "Extract dish information from this speech. " +
                    "Return a JSON array of dish objects, each with keys: " +
                    "name (string), description (string, 1-2 appetizing sentences), " +
                    "price (number in INR, suggest 80-200 for typical homestay dishes), isVeg (boolean). " +
                    "If a single dish is mentioned, return a single-element array. " +
                    "If no specific dish is clear, use the full text as the name. " +
                    "Return ONLY the JSON array, no markdown, no explanation."
                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: return@withContext Result.failure(Exception("Empty response"))
                val cleaned = text.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val dishes = mutableListOf<Dish>()
                try {
                    val jsonArray = JsonParser.parseString(cleaned).asJsonArray
                    for (element in jsonArray) {
                        val obj = element.asJsonObject
                        dishes.add(
                            Dish(
                                name = obj.get("name")?.asString ?: voiceText,
                                description = obj.get("description")?.asString ?: "A delicious homestyle dish.",
                                category = category,
                                price = obj.get("price")?.asDouble ?: 0.0,
                                isVeg = obj.get("isVeg")?.asBoolean ?: true,
                                veg = obj.get("isVeg")?.asBoolean ?: true,
                                spicy = obj.get("isSpicy")?.asBoolean ?: false
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Fallback: treat the entire voice text as a dish name
                    dishes.add(Dish(name = voiceText, category = category))
                }
                Result.success(dishes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Translation service for multiple languages
    suspend fun translateToLocalLanguage(text: String, targetLanguage: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val languageName = when (targetLanguage) {
                    "kn" -> "Kannada"
                    "hi" -> "Hindi"
                    else -> "English"
                }
                if (targetLanguage == "en") {
                    return@withContext Result.success(text)
                }
                val prompt = "Translate the following text to $languageName. " +
                        "Return ONLY the translated text, no explanation:\n\n$text"
                val response = generativeModel.generateContent(prompt)
                Result.success(response.text ?: text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Private helper methods for JSON parsing
    private fun extractJsonValue(json: String, key: String): String? {
        val regex = """"$key"\s*:\\s*"([^"]*?)""""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractJsonBoolean(json: String, key: String): Boolean? {
        val regex = """"$key"\s*:\\s*(true|false)""""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractJsonDouble(json: String, key: String): Double? {
        val regex = """"$key"\s*:\\s*([0-9.]+)""""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}

// Data class for food analysis result
data class FoodAnalysisResult(
    val dishName: String,
    val description: String,
    val category: String,
    val isVeg: Boolean,
    val isSpicy: Boolean,
    val suggestedPrice: Double
)
