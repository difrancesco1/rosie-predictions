// pages/index.js
import { useEffect, useState } from 'react';
import Head from 'next/head';
import Image from 'next/image';
import LoginButton from '../components/auth/LoginButton';
import UserProfile from '../components/auth/UserProfile';
import CreatePredictionForm from '../components/prediction/CreatePredictionForm.js';
import PredictionList from '../components/prediction/PredictionList.js';
import LeagueIntegration from '../components/integrations/LeagueIntegration.js';
import BottomNavigation from '../components/navigation/BottomNavigation';
import SlideUpModal from '../components/navigation/SlideUpModal';
import { isAuthenticated } from '../utils/auth';
import { handleTwitchCallback } from '../services/api';
import { setUserData } from '../utils/auth';
import "../styles/global.css";

export default function Home() {
  const [authenticated, setAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState("Connecting to Twitch...");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [activeContent, setActiveContent] = useState(null);
  const [modalTitle, setModalTitle] = useState('');

  // Check for authentication token initially
  useEffect(() => {
    setAuthenticated(isAuthenticated());
  }, []);

  // Check for auth process in progress
  useEffect(() => {
    const checkAuthInProgress = async () => {
      // Skip if already authenticated
      if (authenticated) return;

      // Check if there's an auth code from the callback
      if (typeof window !== 'undefined') {
        const authCode = sessionStorage.getItem('twitch_auth_code');
        const authInProgress = sessionStorage.getItem('auth_in_progress');
        const authError = sessionStorage.getItem('twitch_auth_error');

        if (authInProgress === 'true' && authCode) {
          try {
            // Show loading screen
            setIsLoading(true);
            setLoadingMessage("Connection successful! Finalizing...");

            // Process the authentication
            const userData = await handleTwitchCallback(authCode);

            // Save the user data
            setUserData(userData);

            // Update status
            setLoadingMessage("Authentication complete!");

            // Update authentication state
            setAuthenticated(true);

            // Clean up session storage
            sessionStorage.removeItem('twitch_auth_code');
            sessionStorage.removeItem('auth_in_progress');

            // Hide loading after a moment
            setTimeout(() => {
              setIsLoading(false);
            }, 1500);
          } catch (error) {
            console.error('Auth error:', error);
            setLoadingMessage("Authentication failed. Please try again.");

            // Clean up session storage
            sessionStorage.removeItem('twitch_auth_code');
            sessionStorage.removeItem('auth_in_progress');

            // Hide loading after showing error
            setTimeout(() => {
              setIsLoading(false);
            }, 2000);
          }
        } else if (authError) {
          // Handle auth error
          setIsLoading(true);
          setLoadingMessage(`Authentication error: ${authError}. Please try again.`);

          // Clean up session storage
          sessionStorage.removeItem('twitch_auth_error');

          // Hide loading after showing error
          setTimeout(() => {
            setIsLoading(false);
          }, 2000);
        }
      }
    };

    checkAuthInProgress();
  }, [authenticated]);

  // Function to handle navigation button click
  const handleNavigate = (contentType) => {
    // Set modal title based on content type
    const titles = {
      create: 'Create Prediction',
      list: 'Your Predictions',
      league: 'League Integration'
    };
    setModalTitle(titles[contentType]);

    // Set the active content type
    setActiveContent(contentType);

    // Open the modal
    setIsModalOpen(true);
  };

  // Function to handle prediction creation success
  const handlePredictionCreated = () => {
    // Close the modal
    setIsModalOpen(false);
  };

  // Render the appropriate content based on active content type
  const renderContent = () => {
    switch (activeContent) {
      case 'create':
        return <CreatePredictionForm onSuccess={handlePredictionCreated} />;
      case 'list':
        return <PredictionList />;
      case 'league':
        return <LeagueIntegration />;
      default:
        return null;
    }
  };

  return (
    <div className="container">
      <Head>
        <title>Twitch Predictions Manager</title>
        <meta name="description" content="Manage your Twitch predictions" />
        <link rel="icon" href="/favicon.ico" />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin />
        <link href="https://fonts.googleapis.com/css2?family=Indie+Flower&family=Pixelify+Sans:wght@400..700&display=swap" rel="stylesheet" />
      </Head>
      <main>
        {authenticated ? (
          <>
            <UserProfile onLogout={() => setAuthenticated(false)} />

            {/* Modal for content */}
            <SlideUpModal
              isOpen={isModalOpen}
              onClose={() => setIsModalOpen(false)}
              title={modalTitle}
            >
              {renderContent()}
            </SlideUpModal>

            {/* Bottom Navigation */}
            <BottomNavigation onNavigate={handleNavigate} />
          </>
        ) : (<>
          <div className='title-container'>
            <h1 className='title-text'>Twitch </h1>&nbsp;<p className='title-text-period'>.</p><h1 className='title-text'>Predictions</h1>
          </div>
          <div className="login-container">
            <div className="hero-image-container">
              <Image
                src="/images/test.PNG"
                alt="Twitch Predictions Hero"
                width={500}
                height={300}
                layout="responsive"
              />
            </div>
            <div className="hero-text-container">
              <h2 className="pixelify-sans-heading">Start</h2>
              <p className="pixelify-sans-body">Predictions</p>
            </div>
            <LoginButton setLoading={setIsLoading} setLoadingMessage={setLoadingMessage} />
          </div>
        </>
        )}
      </main>

      {isLoading && (
        <div className="global-loading-overlay">
          <div className="global-loading-content">
            <div className="loading-gif-container">
              <Image
                src="/images/FLOPPYFISHROSIE.gif"
                alt="Loading..."
                width={150}
                height={150}
                priority
              />
            </div>
            <p className="pixelify-sans-body loading-text">{loadingMessage}</p>
          </div>
        </div>
      )}

      <footer
        href="https://github.com/yourusername/twitch-predictions-manager"
        target="_blank"
        rel="noopener noreferrer"
      >
      </footer>
    </div>
  );
}