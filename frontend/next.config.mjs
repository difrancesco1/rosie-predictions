// next.config.mjs
/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,

    // For Electron to work with static export
    output: process.env.NODE_ENV === 'production' ? 'export' : undefined,

    // Required for static export with images
    images: process.env.NODE_ENV === 'production' ? { unoptimized: true } : {},

    // For production build to work with file:// protocol in Electron
    assetPrefix: process.env.NODE_ENV === 'production' ? './' : undefined,
};

export default nextConfig;