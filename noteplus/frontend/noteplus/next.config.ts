import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  turbopack: {},

  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.BACKEND_URL || 'http://localhost:8080'}/api/:path*`,
      },
    ]
  },

  webpack(config, { dev }) {
    if (dev) {
      config.watchOptions = {
        ignored: /node_modules|\.git|\.next[/\\]cache|target/,
        poll: false,
      }
      config.devtool = false
    }
    return config
  },
}

export default nextConfig
