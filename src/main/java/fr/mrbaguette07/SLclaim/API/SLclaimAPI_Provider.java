package fr.mrbaguette07.SLclaim.API;

import fr.mrbaguette07.SLclaim.SLclaim;

/**
 * Provider class for the SLclaimAPI.
 * This class is used to initialize and provide access to the API implementation.
 */
public class SLclaimAPI_Provider {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Instance of SLclaimAPI */
    private static SLclaimAPI apiInstance;
    
    
    // *******************
    // *  Other Methods  *
    // *******************
    

    /**
     * Initializes the SLclaimAPI with the provided SLclaim instance.
     * This method must be called before accessing the API.
     *
     * @param instance the instance of the SLclaim to use for API initialization
     */
    public static void initialize(SLclaim instance) {
        if (apiInstance == null) {
            apiInstance = new SLclaimAPI_Impl(instance);
        }
    }

    /**
     * Returns the initialized SLclaimAPI instance.
     * Throws an IllegalStateException if the API has not been initialized.
     *
     * @return the initialized SLclaimAPI instance
     * @throws IllegalStateException if the API has not been initialized
     */
    public static SLclaimAPI getAPI() {
        if (apiInstance == null) {
            throw new IllegalStateException("API not initialized. Call initialize() first.");
        }
        return apiInstance;
    }
}
