// components/LoadingScreen.js
import React from 'react';
import Image from 'next/image';

export default function LoadingScreen() {
    return (
        <div className="loading-screen">
            <div className="loading-gif-container">
                <Image
                    src="/images/FLOPPYFISHROSIE.gif"
                    alt="Loading..."
                    width={200}
                    height={150}
                    priority
                />
            </div>
            <p className="loading-text">Connecting to Twitch...</p>
        </div>
    );
}