// pages/auth/callback.js
import { useEffect } from 'react';
import { useRouter } from 'next/router';
import Head from 'next/head';
import Image from 'next/image';

export default function AuthCallback() {
    const router = useRouter();

    useEffect(() => {
        if (router.isReady) {
            // Check if we have the auth code
            if (router.query.code) {
                // Store auth code in sessionStorage
                sessionStorage.setItem('twitch_auth_code', router.query.code);
                sessionStorage.setItem('auth_in_progress', 'true');

                // Detect if we're in a popup window
                const isPopup = window.opener && window.opener !== window;

                if (isPopup) {
                    // If we're in a popup, just close the window after saving the code
                    // The main window will detect this and continue the auth process
                    window.close();
                } else {
                    // If we're not in a popup, redirect back to the main page
                    router.replace('/');
                }
            } else if (router.query.error) {
                // Store error in sessionStorage
                sessionStorage.setItem('twitch_auth_error', router.query.error);

                // Check if we're in a popup
                const isPopup = window.opener && window.opener !== window;

                if (isPopup) {
                    // Close the popup on error
                    window.close();
                } else {
                    // Redirect back to home page
                    router.replace('/');
                }
            }
        }
    }, [router.isReady, router.query, router]);

    // Return a loading screen
    return (
        <>
            <Head>
                <title>Authenticating...</title>
                <link rel="preconnect" href="https://fonts.googleapis.com" />
                <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin />
                <link href="https://fonts.googleapis.com/css2?family=Indie+Flower&family=Pixelify+Sans:wght@400..700&display=swap" rel="stylesheet" />
                <style>{`
          body, html {
            margin: 0;
            padding: 0;
            height: 100%;
            width: 100%;
            overflow: hidden;
          }
          .fullscreen-loader {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgb(254,254,254);
            background: linear-gradient(180deg, rgba(254,254,254,1) 7%, rgba(217,255,146,1) 80%);
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            z-index: 9999;
          }
          .loading-text {
            font-family: "Pixelify Sans", sans-serif;
            font-weight: 400;
            font-size: 18px;
            color: #9146FF;
            margin-top: 15px;
          }
        `}</style>
            </Head>
            <div className="fullscreen-loader">
                <div>
                    <Image
                        src="/images/FLOPPYFISHROSIE.gif"
                        alt="Loading..."
                        width={150}
                        height={150}
                        priority
                    />
                </div>
                <p className="loading-text">Processing authorization...</p>
            </div>
        </>
    );
}