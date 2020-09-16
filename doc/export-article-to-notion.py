from notion.client import *
from md2notion.upload import *

# Follow the instructions at https://github.com/jamalex/notion-py#quickstart to setup Notion.py
client = NotionClient(token_v2="28742461bfce77cc6d585203b48691f5c657a50a8961395660c156742075dd249adeede28414b300ec2d2c6f14abe7a205dddb25830bd64c7d15eea77f5f09369c22e1fd4ed5a529c1eb8bf77d3e")
page = client.get_block("https://www.notion.so/pbaille/code2-7c4ae5c5431f42eb91ac7eceae3c858f")

with open("article.md", "r", encoding="utf-8") as mdFile:
    newPage = page.children.add_new(PageBlock, title="Crux article")
    upload(mdFile, newPage) #Appends the converted contents of TestMarkdown.md to newPage