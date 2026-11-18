import type { Product } from '@makon/api-client';

const bySlug: Record<string, string> = {
  'nova-x1': '/products/electronics.png',
  'airbuds-flow': '/products/audio.png',
  'luma-desk': '/products/home.png',
  'sahar-no-04': '/products/beauty.png',
  'move-bottle': '/products/sport.png',
  'orbit-watch-s': '/products/watch.png',
  'studio-headphones': '/products/audio.png',
  'mizu-ceramic-set': '/products/ceramic.png',
  'aura-skin-serum': '/products/beauty.png',
  'flex-yoga-mat': '/products/sport.png',
  'halo-desk-lamp': '/products/home.png',
  'pulse-mini-speaker': '/products/audio.png',
};

const byCategory: Record<string, string> = {
  '2ac43520-5c23-4bb9-8dd3-388b18bcd6d1': '/products/electronics.png',
  '6e91fc90-dcfd-4345-80d0-48b0bc607e9d': '/products/electronics.png',
  '40000000-0000-0000-0000-000000000002': '/products/home.png',
  '40000000-0000-0000-0000-000000000003': '/products/beauty.png',
  '40000000-0000-0000-0000-000000000004': '/products/sport.png',
};

export function fallbackProductImage(product: Product) {
  return bySlug[product.slug] || byCategory[product.categoryId] || '/products/electronics.png';
}
