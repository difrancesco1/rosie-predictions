// pages/index.js
import { useEffect, useState } from 'react';
import Head from 'next/head';
import LoginButton from '../components/auth/LoginButton';
import UserProfile from '../components/auth/UserProfile';
import { isAuthenticated } from '../utils/auth';
// import './index.css';


export default function Home() {
    const [authenticated, setAuthenticated] = useState(false);

    useEffect(() => {
        // Check if the user is authenticated
        const checkAuth = () => {
            setAuthenticated(isAuthenticated());
        };

        checkAuth();
    }, []);

    return (
        <div className="container">
            <Head>
                <title>Twitch Predictions Manager</title>
                <meta name="description" content="Manage your Twitch predictions" />
                <link rel="icon" href="/favicon.ico" />
            </Head>

            <main className="main">
                <h1 className="title">
                    Twitch Predictions Manager
                </h1>

                <p className="description">
                    Create and manage predictions for your Twitch channel
                </p>

                {authenticated ? (
                    <>
                        <UserProfile onLogout={() => setAuthenticated(false)} />
                        <div className="content">
                            <p>You are now logged in! Soon you&apos;ll be able to manage your predictions here.</p>
                        </div>
                    </>
                ) : (
                    <div className="auth-section">
                        <p>Please login with your Twitch account to get started:</p>
                        <LoginButton />
                    </div>
                )}
            </main>

            <footer className="footer">
                <a
                    href="https://github.com/yourusername/twitch-predictions-manager"
                    target="_blank"
                    rel="noopener noreferrer"
                >
                    Created with Next.js, Spring Boot, and Electron
                </a>
            </footer>

            <style jsx>{`
        .container {
          min-height: 100vh;
          padding: 0 0.5rem;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
        }

        .main {
          padding: 5rem 0;
          flex: 1;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
          width: 100%;
          max-width: 800px;
        }

        .footer {
          width: 100%;
          height: 100px;
          border-top: 1px solid #eaeaea;
          display: flex;
          justify-content: center;
          align-items: center;
        }

        .title {
          margin: 0;
          line-height: 1.15;
          font-size: 4rem;
          text-align: center;
        }

        .description {
          line-height: 1.5;
          font-size: 1.5rem;
          text-align: center;
        }

        .auth-section {
          margin-top: 2rem;
          text-align: center;
        }

        .content {
          width: 100%;
          margin-top: 2rem;
        }
      `}</style>

            <style jsx global>{`
        html,
        body {
          padding: 0;
          margin: 0;
          font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto,
            Oxygen, Ubuntu, Cantarell, Fira Sans, Droid Sans, Helvetica Neue,
            sans-serif;
        }

        * {
          box-sizing: border-box;
        }
      `}</style>
        </div>
    );
}