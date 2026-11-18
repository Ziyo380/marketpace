//@ts-check

const path = require('path');

/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${process.env.API_UPSTREAM || 'http://127.0.0.1:8080'}/api/:path*` }];
  },
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: 'flagsapi.com', pathname: '/**' },
    ],
  },
  turbopack: {
    resolveAlias: {
      'next-intl/config': './src/i18n/request.ts',
    },
  },
  webpack(config) {
    config.resolve.alias['next-intl/config'] = path.resolve(__dirname, 'src/i18n/request.ts');
    return config;
  },
};

module.exports = nextConfig;
