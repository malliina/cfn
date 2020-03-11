# Static website

Sets up a static website at a custom domain using:

- Route 53 for DNS (assumes you have set up a hosted zone)
- CloudFront for CDN and HTTPS support
- S3 for content storage

## Notes

You must not configure CloudFront to cache based on the `Host` header: 

https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/header-caching.html#header-caching-web-selecting

Use custom headers to restrict bucket access to CloudFront only:

https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-overview.html#forward-custom-headers-restrict-access

"For CloudFront to get your files from a custom origin, the files must be publicly accessible. But by using custom headers, you can restrict access to your content so that users can access it only through CloudFront, not directly."

You do not need togrant public read access when uploading objects.
